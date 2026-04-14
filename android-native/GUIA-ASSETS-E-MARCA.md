# Guia: ícones, logo e assets visuais (Well Paid — Android)

Este documento reúne **o que falta**, **onde colocar ficheiros** e **como atualizar** o projeto enquanto o desenvolvimento avança. Podes ir cumprindo os passos à tua cadência.

---

## 1. Situação atual (resumo)

| Área | Onde está | Notas |
|------|-----------|--------|
| Ícone da app (launcher) | `app/src/main/res/mipmap-anydpi-v26/` + `drawable/ic_launcher_*` | Ícone **adaptativo** (API 26+). Fundo e primeiro plano são **vetores** em `drawable/` porque a pasta `android/` na raiz do repo ainda não trazia os PNG/WebP de `ic_launcher_foreground` em todas as densidades. |
| Logo no ecrã de login | `app/src/main/res/drawable/ic_logo.xml` | Vetor temporário (teal + marca). Ideal substituir por **PNG/WebP** de alta qualidade quando tiveres o ficheiro final. |
| Pasta `android/` (raiz do repo) | `Well Paid/android/` | Contém XMLs de exemplo (fundo verde “template” + referência a `@mipmap/ic_launcher_foreground`). Serve de **referência** ou de origem quando copiares rasters exportados do Figma/Android Studio. |

O **`minSdk` do projeto é 26**, por isso o ícone adaptativo em `mipmap-anydpi-v26` é suficiente para o launcher. Se no futuro baixares o `minSdk`, serão necessários ícones “legados” em `mipmap-mdpi`, `hdpi`, etc. (sem adaptativo).

---

## 2. Ícone da app (launcher adaptativo)

### O que é preciso (quando quiseres produção final)

1. **Fundo** (`ic_launcher_background`): normalmente cor sólida ou gradiente simples — já tens vetor em `drawable/ic_launcher_background.xml` (teal Well Paid).
2. **Primeiro plano** (`ic_launcher_foreground`): o símbolo/logo **dentro da zona segura** (~66% do quadrado de 108dp). Pode ser:
   - **Vetor** (`drawable/ic_launcher_foreground.xml`) — como agora; ou
   - **Raster** (recomendado para detalhe fino): WebP/PNG em `mipmap-mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi` com o nome `ic_launcher_foreground.webp` (ou `.png`).

### Onde colocar os ficheiros raster

Copia para o módulo da app (não só para `Well Paid/android/`):

```
android-native/app/src/main/res/
  mipmap-mdpi/ic_launcher_foreground.webp
  mipmap-hdpi/ic_launcher_foreground.webp
  mipmap-xhdpi/ic_launcher_foreground.webp
  mipmap-xxhdpi/ic_launcher_foreground.webp
  mipmap-xxxhdpi/ic_launcher_foreground.webp
```

Tamanhos orientativos (px, lado do quadrado):

| Densidade | Tamanho típico do *foreground* |
|-----------|---------------------------------|
| mdpi | 108 px |
| hdpi | 162 px |
| xhdpi | 216 px |
| xxhdpi | 324 px |
| xxxhdpi | 432 px |

(Valores alinhados ao template do Android Studio; confirma no teu export.)

### Como ligar o raster ao ícone adaptativo

1. Abre `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`.
2. Altera o `foreground` de `@drawable/ic_launcher_foreground` para **`@mipmap/ic_launcher_foreground`** (quando os ficheiros acima existirem).
3. Opcional: remove ou simplifica `drawable/ic_launcher_foreground.xml` para não haver dois recursos com o mesmo “papel” (o build usa o que referenciares no XML).

### Ferramentas úteis

- **Android Studio**: *Image Asset* (clique direito em `res` → *New* → *Image Asset*) — gera pastas `mipmap-*` e XMLs adaptativos.
- Export a partir do **Figma**: exporta camadas em PNG/WebP nas dimensões acima ou usa um plugin de “Android launcher icons”.

---

## 3. Logo no ecrã de login (e noutros ecrãs)

### Estado atual

- O composable **`LoginScreen`** usa `painterResource(R.drawable.ic_logo)`.
- `ic_logo.xml` é um **vector drawable** de marca.

### Como passar para bitmap (recomendado para o logo final)

**Opção A — Substituir o recurso (menos mudanças de código)**  
1. Adiciona `logo_well_paid.png` ou `.webp` em:
   - `app/src/main/res/drawable-nodpi/` (uma só densidade, sem escala automática — bom para logos complexos), **ou**
   - `drawable-xxhdpi/` (ou várias densidades se quiseres nitidez em todos os ecrãs).
2. Remove ou renomeia o `ic_logo.xml` antigo (evita conflito de nome).
3. No código, troca `R.drawable.ic_logo` por `R.drawable.logo_well_paid` em `LoginScreen.kt` (e em qualquer outro sítio que uses o logo).

**Opção B — Manter o nome `ic_logo`**  
1. Apaga `ic_logo.xml`.
2. Coloca `ic_logo.png` / `ic_logo.webp` na pasta `drawable-nodpi` (mesmo nome base `ic_logo`).
3. Não precisas alterar o Kotlin se o nome do recurso continuar `ic_logo`.

### Acessibilidade

- Mantém `contentDescription` (já usamos `logo_content_description` em `strings.xml`). Ajusta o texto se o marketing pedir outra formulação.

---

## 4. Pasta `android/` na raiz do repositório

- Podes usá-la como **staging**: colocar aqui exports do designer antes de os copiares para `android-native/app/src/main/res/`.
- O Gradle **não** lê automaticamente `Well Paid/android/` — tudo o que a app usar tem de estar sob **`android-native/app/src/main/res/`** (ou outro módulo Android que declares).

Sugestão de fluxo de trabalho:

1. Designer exporta → `Well Paid/android/` (ou direto para `android-native/.../res/`).
2. Tu copias/ajustas para `android-native/app/src/main/res/`.
3. Atualizas os XML em `mipmap-anydpi-v26/` e `drawable/` conforme as secções 2 e 3.
4. Fazes *Sync* no Android Studio e um *Clean/Rebuild*.

---

## 5. Checklist rápido (para ires marcando)

- [ ] `ic_launcher_foreground` em **todas** as densidades `mipmap-*` (WebP preferível).
- [ ] `ic_launcher.xml` / `ic_launcher_round.xml` a apontar para `@mipmap/ic_launcher_foreground` (se usares raster).
- [ ] Revisão visual do ícone em **círculo** e **squircle** no launcher (fabricantes variam).
- [ ] Logo final no login (`drawable-nodpi` ou múltiplas densidades).
- [ ] `strings.xml`: texto do `logo_content_description` alinhado com produto/acessibilidade.
- [ ] (Opcional) *Splash screen* API 12+ com o mesmo branding — quando implementares, reutiliza o mesmo asset do logo.

---

## 6. Onde está o código relevante

| Ficheiro | Função |
|----------|--------|
| `app/src/main/AndroidManifest.xml` | `android:icon` / `android:roundIcon` → `@mipmap/ic_launcher` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Definição do ícone adaptativo |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Fundo do adaptativo |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Primeiro plano em vetor (até haver `mipmap`) |
| `app/src/main/res/drawable/ic_logo.xml` | Logo usado no login |
| `app/src/main/java/.../ui/login/LoginScreen.kt` | `Image` + `painterResource` do logo |
| `app/src/main/res/values/strings.xml` | `logo_content_description` e restantes strings |

---

## 7. Cores da marca (referência)

- Teal principal usado nos vetores atuais: **`#0D9488`** (alinhado ao tema Compose “Well Paid”).
- Ao gerar novos assets, mantém contraste **WCAG** com texto branco sobre o teal e verifica em modo claro/escuro se no futuro adicionares tema escuro.

---

*Última atualização: alinhado ao estado do módulo `android-native` e à pasta `android/` na raiz do repo. Atualiza este guia quando mudares `minSdk`, nomes de recursos ou fluxo de design.*
