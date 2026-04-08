# Well Paid — Guia de Deploy e Hospedagem (Produção)

Documento prático para o momento em que o projeto estiver finalizado e pronto para publicação.

## 1) Como o sistema roda no fim

- O APK contém somente o app Flutter (`mobile/`), compilado para Android.
- O backend FastAPI (`backend/`) roda em servidor separado (cloud/VPS/PaaS).
- O app conversa com o backend por HTTPS (JWT access/refresh).
- O backend conversa com o PostgreSQL (Neon).
- Em produção, o app **não** executa Python; ele consome API remota.

Fluxo:
1. Utilizador abre o app.
2. App faz login na API.
3. API valida, aplica regras de negócio e grava no Neon.
4. API retorna JSON.
5. App atualiza UI.

---

## 2) O que você precisa ter antes de publicar

## 2.1 Contas/serviços

- Banco PostgreSQL: Neon (já existente).
- Hospedagem do backend: escolher uma opção (ver secção 4).
- Domínio/API: opcional mas recomendado (`api.seudominio.com`).
- SMTP para e-mails transacionais (reset de senha etc.).
- Conta Play Console (quando for publicar oficialmente).

## 2.2 Itens técnicos obrigatórios

- Backend com migrações Alembic aplicadas até o head.
- Variáveis de ambiente configuradas no servidor.
- HTTPS ativo (certificado válido).
- CORS ajustado para app mobile.
- Logs e healthcheck do backend funcionando.
- Build release do app apontando para URL de produção.

---

## 3) Quais “documentos/artefatos” você vai precisar hospedar

Importante: você não hospeda `.md`. O que vai para produção é código + configuração.

## 3.1 Backend (hospedado)

- Pasta `backend/` (código FastAPI).
- Dependências Python (`requirements`/lockfile usado no projeto).
- Script/entrypoint de execução (`uvicorn ...`).
- Migrações Alembic.

## 3.2 Banco (hospedado no Neon)

- Estrutura do schema (via Alembic).
- Dados de produção.
- Política de backup/restore.

## 3.3 App mobile (distribuição)

- APK/AAB de release.
- Configuração de endpoint de API de produção.
- Assinatura de release (keystore).

## 3.4 Configuração sensível (não versionar)

- `.env` real de produção (somente no servidor/plataforma).
- Tokens/chaves/segredos.
- Nunca commitar credenciais em README/docs.

---

## 4) Alternativas de hospedagem do backend (com Neon)

## Opção A — Render (mais simples para começar)

**Bom para:** MVP e operação simples.

- Prós:
  - Deploy fácil a partir de repositório.
  - SSL e domínio custom simplificados.
  - Boa experiência para FastAPI.
- Contras:
  - Plano grátis pode hibernar (cold start).
  - Menos controle fino que VPS.

## Opção B — Railway / Fly.io (PaaS flexível)

**Bom para:** evolução com menos atrito operacional.

- Prós:
  - Deploy rápido, boa DX.
  - Variáveis e logs centralizados.
  - Escalabilidade melhor que setups básicos.
- Contras:
  - Custos podem subir com tráfego.
  - Curva de entendimento da plataforma.

## Opção C — VPS (Hetzner / Contabo / DigitalOcean)

**Bom para:** custo previsível e maior controle.

- Prós:
  - Controle total do servidor.
  - Custo/benefício forte em produção estável.
- Contras:
  - Você administra tudo (Nginx, segurança, updates, monitoramento).
  - Exige mais tempo de operação.

## Opção D — Google Cloud Run / AWS App Runner / Azure Container Apps

**Bom para:** arquitetura mais “profissional” com container.

- Prós:
  - Escala gerenciada.
  - Integração com observabilidade/cloud nativa.
- Contras:
  - Mais complexo no início.
  - Curva e custos de cloud pública.

Recomendação prática para o Well Paid agora:
1. Começar com **Render** ou **Railway**.
2. Manter Neon como banco.
3. Migrar para VPS/cloud mais robusta só quando uso/custo justificar.

---

## 5) Checklist de deploy (passo a passo)

## 5.1 Preparar backend para produção

- [ ] Revisar variáveis de ambiente com placeholders:
  - `DATABASE_URL=postgresql+psycopg://USUARIO:SENHA@HOST/DB?sslmode=require`
  - `SECRET_KEY=coloque-um-segredo-forte-aqui`
  - `ACCESS_TOKEN_EXPIRE_MINUTES=60`
  - `REFRESH_TOKEN_EXPIRE_DAYS=30`
  - `SMTP_HOST=smtp.seuprovedor.com`
  - `SMTP_PORT=587`
  - `SMTP_USER=seu_usuario`
  - `SMTP_PASSWORD=sua_senha`
  - `MAIL_FROM=no-reply@seudominio.com`
- [ ] Configurar CORS.
- [ ] Configurar comando de start da API.
- [ ] Configurar endpoint de healthcheck.

## 5.2 Publicar backend

- [ ] Subir backend na plataforma escolhida.
- [ ] Inserir variáveis de ambiente no painel da plataforma.
- [ ] Ligar backend ao Neon.
- [ ] Executar migrações Alembic no ambiente de produção.
- [ ] Validar `GET /health` e rotas principais.

## 5.3 Preparar app mobile release

- [ ] Definir URL de API de produção no app.
- [ ] Gerar keystore de assinatura release.
- [ ] Build:
  - `flutter build apk --release`
  - ou `flutter build appbundle --release`
- [ ] Testar app release real com API de produção.

## 5.4 Publicação

- [ ] Teste interno (alpha/internal testing).
- [ ] Corrigir bugs críticos.
- [ ] Publicar na Play Store.

---

## 6) Operação pós-lançamento (mínimo necessário)

- Monitorar:
  - uptime da API,
  - erro 5xx,
  - tempo de resposta,
  - falhas de login/refresh.
- Banco:
  - acompanhar consumo Neon,
  - validar backup e estratégia de restore.
- Segurança:
  - rotacionar segredos periodicamente,
  - nunca expor tokens/senhas em logs.
- Release:
  - versionamento do app,
  - changelog simples por versão.

---

## 7) Riscos comuns e prevenção

- **Cold start** em plano grátis -> usar plano sem hibernação quando tiver utilizadores ativos.
- **Timeout na API** -> revisar limites, queries e índices.
- **Falha de e-mail SMTP** -> monitorar envio e fallback.
- **Config errada de URL no app** -> separar claramente dev/staging/prod.
- **Migração quebrada** -> testar Alembic em ambiente staging antes de produção.

---

## 8) Plano recomendado para você (estado atual: Neon já pronto)

Fase 1 (rápida):
1. Backend no Render/Railway.
2. Neon em produção.
3. Domínio opcional inicial (URL da plataforma).
4. APK interno para testes reais.

Fase 2 (estável):
1. Domínio próprio + SSL.
2. Monitoramento e logs melhores.
3. Processo de release contínuo.

Fase 3 (escala):
1. Revisar custos Neon + backend.
2. Avaliar migração de infraestrutura se necessário.

---

## 9) Entregáveis finais esperados quando for lançar

- Backend online e estável.
- Banco Neon com migrações aplicadas.
- App release conectado à API de produção.
- SMTP funcional.
- Checklist de operação mínima validado.

Com isso, o Well Paid fica pronto para uso real sem depender do ambiente local.
