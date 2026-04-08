# -*- coding: utf-8 -*-
"""
Blueprint de Despesas - renderiza a tela e processa inclusão/edição de despesas.
Usa funções do app (add_expense, get_recent_expenses, update_expense, etc.) via import tardio.
Erros são registrados no log específico da página (logs/despesas_errors.log).
"""
import logging
from flask import Blueprint, request, session, render_template, flash, redirect, url_for, jsonify
from extensions import get_user_settings, login_required
from datetime import datetime

from locale_safe import parse_br_number, format_br_currency

despesas_logger = logging.getLogger('despesas')

despesas_bp = Blueprint('despesas', __name__, url_prefix='/despesas')


def _format_currency_brl(value):
    """Formata valor como R$ no padrão pt-BR (ex: R$ 1.270,00). Usa locale_safe + correção centavos."""
    if value is None:
        return "R$ 0,00"
    try:
        n = float(value)
        if n >= 100000 and n == int(n):
            n = round(n / 100.0, 2)
        return format_br_currency(n)
    except (ValueError, TypeError):
        return "R$ 0,00"


@despesas_bp.route('/', methods=['GET', 'POST'])
@login_required
def despesas():
    # Importação tardia para evitar import circular (app importa este blueprint)
    from app import (
        add_expense,
        get_recent_expenses,
        update_expense,
        get_notifications,
        get_sharing_permissions,
        normalize_date,
    )

    username = session['username']
    settings = get_user_settings(username)
    sharing_permissions = get_sharing_permissions(username)
    notifications = get_notifications(username)
    today = datetime.now().date()

    is_ajax = request.headers.get('X-Requested-With') == 'XMLHttpRequest'

    def _json_ok(msg):
        return jsonify({'status': 'success', 'message': msg}) if is_ajax else redirect(url_for('despesas.despesas'))

    def _json_err(msg, code=400):
        if is_ajax:
            return jsonify({'status': 'error', 'message': msg}), code
        flash(msg, 'error')
        return redirect(url_for('despesas.despesas'))

    if request.method == 'POST':
        edit_id = request.form.get('edit_expense_id', '').strip()
        if edit_id:
            # Edição de despesa existente
            try:
                expense_id = int(edit_id)
            except ValueError:
                despesas_logger.warning("Despesas: ID de despesa inválido no formulário: %r", edit_id)
                return _json_err('ID de despesa inválido.')

            data = request.form.get('data')
            descricao = request.form.get('descricao', '').strip()
            try:
                valor = parse_br_number(request.form.get('valor') or '0')
            except ValueError:
                return _json_err('Valor inválido.')
            categoria = request.form.get('tipo', 'outros').lower()
            parcelas = request.form.get('parcelas', '1')
            due_date = request.form.get('due_date') or None
            recorrente = request.form.get('recorrente') == 'on'
            total_parcels = int(parcelas) if parcelas.isdigit() else 1
            parcelas_faltam = request.form.get('parcelas_faltam')
            data_inicial = request.form.get('data_inicial') or None
            is_andamento = request.form.get('is_andamento') == 'on'
            if parcelas_faltam is not None and str(parcelas_faltam).isdigit():
                parcelas_faltam = int(parcelas_faltam)
            else:
                parcelas_faltam = None

            if not descricao or valor <= 0:
                return _json_err('Preencha descrição e valor válido.')
            ok = update_expense(
                expense_id, username,
                date=data, description=descricao, amount=valor, category=categoria,
                due_date=due_date, recurring=recorrente, total_parcels=total_parcels,
                is_andamento=is_andamento, parcelas_faltam=parcelas_faltam, data_inicial=data_inicial
            )
            if ok:
                if not is_ajax:
                    flash('Despesa atualizada com sucesso.', 'success')
                return _json_ok('Despesa atualizada com sucesso.')
            despesas_logger.error("Despesas: falha ao atualizar despesa ID %s para usuário %s", expense_id, username)
            return _json_err('Erro ao atualizar despesa.', 500)

        # Nova despesa
        data = request.form.get('data')
        descricao = request.form.get('descricao', '').strip()
        try:
            valor = parse_br_number(request.form.get('valor') or '0')
        except ValueError:
            return _json_err('Valor inválido.')
        categoria = request.form.get('tipo', 'outros').lower()
        parcelas = request.form.get('parcelas', '1')
        due_date = request.form.get('due_date') or None
        recorrente = request.form.get('recorrente') == 'on'
        esta_paga = request.form.get('esta_paga') == 'on'
        tem_vencimento = request.form.get('tem_vencimento') == 'on'
        is_andamento = request.form.get('is_andamento') == 'on'
        parcelas_faltam = request.form.get('parcelas_faltam')
        data_inicial = request.form.get('data_inicial') or None
        compartilhada = request.form.get('compartilhada') == 'on'
        shared_with = request.form.get('shared_with') or None
        sharing_mode = request.form.get('sharing_mode') or 'equal'
        shared_amount_raw = request.form.get('shared_amount')
        shared_amount = None
        if shared_amount_raw:
            try:
                shared_amount = parse_br_number(shared_amount_raw)
            except ValueError:
                pass

        total_parcels = int(parcelas) if parcelas.isdigit() else 1
        if parcelas_faltam is not None and str(parcelas_faltam).isdigit():
            parcelas_faltam = int(parcelas_faltam)
        else:
            parcelas_faltam = 0

        status = 'Paga' if esta_paga else 'Pendente'
        paid_date = normalize_date(data) if esta_paga else None

        if not descricao or valor <= 0:
            return _json_err('Preencha descrição e valor válido.')

        try:
            add_expense(
                username,
                date=data,
                description=descricao,
                amount=valor,
                category=categoria,
                due_date=due_date if tem_vencimento else None,
                total_parcels=total_parcels,
                recurring=recorrente,
                status=status,
                shared_with=shared_with if compartilhada else None,
                sharing_mode=sharing_mode if compartilhada else None,
                shared_amount=shared_amount,
                paid_date=paid_date,
                is_andamento=is_andamento,
                parcelas_faltam=parcelas_faltam if is_andamento else None,
                data_inicial=normalize_date(data_inicial) if data_inicial else None,
            )
            if not is_ajax:
                flash('Despesa registrada com sucesso.', 'success')
            return _json_ok('Despesa registrada com sucesso.')
        except Exception as e:
            despesas_logger.exception("Despesas: erro ao registrar despesa para %s: %s", username, e)
            return _json_err(str(e) or 'Erro ao registrar despesa.', 500)

    expenses = get_recent_expenses(username)
    # Lista separada de valores já formatados em R$ 1.270,00 (não altera a estrutura de expenses)
    formatted_valor_list = [_format_currency_brl(row[3]) for row in expenses]
    return render_template(
        'despesas.html',
        expenses=expenses,
        formatted_valor_list=formatted_valor_list,
        today=today,
        settings=settings,
        sharing_permissions=sharing_permissions,
        notifications=notifications,
    )
