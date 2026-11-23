# DanPlus 2.1.0 — pacote canônico de preparação

**Versão escolhida pelo proprietário em 19/11/2026:** próxima versão **2.1.0**, partindo da **2.0.2/1644** homologada. **Não há APK 2.1.0, build, teste ou release executado.** Não alterar versionCode nem editar manifests por mera escolha do nome.

## Base que deve ser preservada no PC
- Fonte oficial de desenvolvimento: `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`, repositório `https://github.com/danilopagotto82/DanPlus`.
- APKs de referência da 2.0.2, hashes e instruções: `D:\APK\DanPlus_2_0_2_HOMOLOGADO`. Duplicatas antigas descartáveis por autorização expressa do proprietário, após verificar cópia idêntica.
- Origem para futuras instalações OTA anteriores: `https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases/tag/2.0.2`; manter o repositório antigo ativo.
- Chave de assinatura Shield (não compartilhar/publicar): `C:\Users\danil\AppData\Local\DanPlus\Shield\Signing\danplus-shield-release.p12`. Confirmar alias/senha e **impressão digital antes do próximo build**; presença do arquivo não garante assinatura compatível.
- Chave Fire (preservar): `C:\Users\danil\.android\debug.keystore`. Impressão digital conferida contra APK Fire 2.0.2: **PASS** via apksigner/keytool.
- Configuração privada: `local.properties` na fonte oficial é ignorada pelo Git e foi copiada com SHA256 idêntico da fonte OTA; backup fora do Git em `C:\Users\danil\AppData\Local\DanPlus\Protected_2_1_0`. Nunca imprimir valores. Não contém as configurações de assinatura Shield nem a chave fallback YouTube.
- SDK/JDK/Gradle e arquivos AAR, bibliotecas C++ e libdovi da fonte oficial: manter. O `libmpv-android` legado é um gitlink não importado, mas a dependência utilizada no Gradle atual é por Maven (`io.github.abdallahmehiz:mpv-android-lib:0.1.12`); validar dependências com Gradle antes de presumir bloqueio.
- Preservar integralmente projetos **independentes** (MasterFinance, ÓRBITA, DAN TV, DaNEXUS e Central).

## Mudanças já registradas — execução planejada, não implementada

Consultar **GEMINI.md**, `docs/danplus/gemini-planejamento/INDICE.md`, `DP_CODEX_01_MEGALOTES_E_GATES_20261117.md`, `DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md` e `PROXIMA_VERSAO_PRONTIDAO_20261119.md`. Os 17 documentos originais foram confrontados por SHA256 com o PC em 19/11, todos iguais.

1. **ML00 — precheck e reconciliação**: verificar baseline real 2.0.2 vs fonte sanitizada; build-signing, libmpv/Maven, RAM/Fire, crash Detalhes ainda existente ou não; comparar itens já implementados para evitar duplicidade. Apenas leitura/inventário.
2. **ML01 — estabilidade se bug comprovado**: investigar crash Compose Detalhes (relatado originalmente na 2.0.0). Só aplicar correção se for reproduzido ou se houver evidência atual.
3. **ML02 — UX/Focus Dock**: LOGO-PTBR-01 no Descobrir/Biblioteca (prioridade PT-BR, fallback idioma **original oficial** da obra, nunca inglês por padrão); BUG-BACKDROP-01 (Descobrir/Indicações sem precisar visitar Detalhes); BUG-THUMB-01 (miniatura cortada sem trocar gerador); Seekr/DPAD.
4. **ML03–06**: calendário/séries e Trakt/Simkl, progressos e históricos; recuperação inteligente de fontes do mesmo addon; player/cache/Fire, legendas/áudio/capítulos; revisão visual de pedidos MUD/REQ16 já parcialmente entregues.
5. **ML07**: RLS/indicações, relatórios, backup e perfis com teste sintético e gate separado para Supabase real. Não publicar segredos.
6. **ML08**: trailers YouTube prioritariamente PT-BR com fallback e cache Supabase, missão separada. A chave fixa histórica foi retirada da nova fonte; fallback alternativo local opcional **não testado**. Não copiar chaves para Git.
7. **ML09**: OTA 2.0.2 já homologada; para 2.1.0 preservar assinaturas, versionCode >1644, os dois nomes/formats por ABI e canais GitHub antigo e novo até validar clientes legados.
8. **ML10**: regressão e smoke reais na Shield e Fire antes do release 2.1.0.
9. **ML11 opcional**: comparação automatizada Nuvio Original + ARC sem IA recorrente, sem merges/releases automáticos.

**Decisões preservadas**: matriz 146 com 36 decisões e 110 pendentes; RS-06/B2-06 pendentes; A2-35 não recomendar; 14 REQ16 sem duplicação de MUD. **Planos ≠ patches.**

## UI-HEADER-01 — DanPlus Signature Glass (APROVADO EM 23/11/2026; DP-ML06)

**Decisão do proprietário:** substituir visualmente o cabeçalho da Home pelo layout flutuante aprovado em prévia nesta conversa, mantendo o restante da interface homologada. **Planejado e aprovado para a 2.1.0; NÃO CODIFICADO, NÃO TESTADO, SEM BUILD.** Não criar megalote independente: integrar ao acabamento de UI do **DP-ML06**, depois da reconciliação da DP-ML00. Não duplicar os componentes MUD-003 (avatares), MUD-004 (navegação) e MUD-005 (relógio).

### Composição homologada da barra superior
1. **Logo DanPlus isolado à extrema esquerda**, fora da barra principal, sobre fundo transparente, sem cápsula/caixa própria.
2. **Menu de navegação principal** em cápsula horizontal única, flutuante, cantos bem arredondados, vidro preto/fumê translúcido, contorno/reflexo sutil; preservar **os menus, rótulos, traduções, ordem e ações reais do DanPlus** verificados no código — a prévia com rótulos em inglês é apenas referência visual, não lista funcional para copiar. **Não** incluir Configurações nesse menu.
3. **Item selecionado/focado** identificado por cápsula interna levemente mais clara, sem brilho exagerado; transições discretas compatíveis com DPAD.
4. **Relógio separado da cápsula principal**, somente caracteres legíveis sobre fundo totalmente transparente, na parte superior direita, preservando fuso, preferência 12/24 horas, atualização e MUD-005 já existentes.
5. **À extrema direita, cápsula menor exclusiva** com engrenagem **Configurações** à esquerda e avatar do **perfil ativo** à direita. **Decisão final explícita:** esta cápsula deve ser **MUITO mais translúcida do que a barra principal, quase transparente**, com preenchimento mínimo e contorno quase imperceptível; engrenagem/avatar devem continuar nítidos e acessíveis pelo controle remoto. Não aplicar esse nível extremo de transparência à barra central.
6. Proteger a arte hero/backdrop, sinopse, cards, áreas de segurança e hierarquia visual já aprovadas. Não remodelar Home nem alterar lógica de dados sob pretexto desta mudança.

### Diretrizes técnicas e aceite futuro
- Localizar e **reutilizar** componentes reais de cabeçalho, navegação, relógio e perfil (Kotlin/Compose) após inventário; preferir patch mínimo em branch/worktree isolada, sem mexer na 2.0.2 homologada nem alterar identidade/signing/OTA.
- Adaptar margens, dimensões e legibilidade a **Shield Pro 2017 e Fire TV 4K Max 2ª**, com TV/overscan e tamanhos de texto; navegação **DPAD completa** (esquerda/direita, foco inicial, retorno, engrenagem, mudança de perfil), sem elemento inalcançável.
- Preferir opacidade/transparência simples; usar desfoque de fundo **somente se medido e sem regressão** de FPS/RAM no Fire. Não introduzir efeito custoso apenas para reproduzir conceito.
- Fazer capturas **da Home real** em Shield e Fire e comparar lado a lado com a referência visual aprovada de 23/11/2026; conferir logo fora da barra, menu central, relógio sem fundo e especialmente a **cápsula de perfil quase invisível**; validar contraste sobre backdrops claros/escuros, foco, acessibilidade e ausência de clipping.
- Entrar no **megalote único de UI** junto às demais melhorias já priorizadas; **não gerar APK/build isolado** para este cabeçalho. Sem teste físico e comparação visual, status **NÃO TESTADO**, jamais PASS.

## Segurança/qualidade e pontos de parada
- Próxima missão recomendada: **DP-ML00 somente leitura**, em GPT-6 Alto + Commander; Codex Alto só para patch Kotlin/Compose. Antes de começar, verificar `AGENTS.md` de D:\APK, branch, HEAD, working tree, processos concorrentes, dependências e evidências reais.
- Para codificar: branch/worktree NOVA criada da fonte oficial limpa. Preservar a main. Sem trocar applicationId, certificados, assinatura, URL OTA ou banco de produção.
- Assinatura Shield / certificado Fire, Gradle build e teste de fallback YouTube: ainda não homologados para **nova fonte sanitizada**.
- 2 alertas Google API Key históricos permanecem no repositório legado. O repositório oficial novo não tinha alertas abertos na última consulta; não importar histórico.
- Sete workflows herdados desativados; não reativar sem revisão.
- Critério de entrega: PRECHECK → PLANO → PATCH → TESTE FOCADO → BUILD → SMOKE FIRE/SHIELD → DIFF/SEGREDOS → gate de publicação. **Não dizer PASS para o que não foi testado.**

## Limpeza expressamente autorizada
O proprietário autorizou descartar **todas as cópias antigas locais do DanPlus**, inclusive branches com alterações não commitadas, por considerar suficiente a 2.0.2 publicada e o plano futuro. Preservar, porém, os itens mínimos de assinatura, fonte oficial, plano, APKs 2.0.2 e arquivos de outros projetos listados acima. Registrar os diretórios realmente excluídos e o espaço efetivamente recuperado, sem inventar sucesso.


## Encerramento da limpeza local — evidência de 19/11/2026

**Executado e confirmado:** o proprietário autorizou descartar definitivamente todas as cópias antigas locais do DanPlus, inclusive alterações dirty. As worktrees e raízes anteriores foram eliminadas; em `D:\APK` restam somente:
- `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119` — fonte oficial para 2.1.0, Git limpo, esta documentação e Gemini;
- `D:\APK\DanPlus_2_0_2_HOMOLOGADO` — dois APKs homologados 2.0.2, hashes verificados e log de exclusão;
- `D:\APK\AGENTS.md` — instruções locais atualizadas.

O código exato da ponte 2.0.2 continua no **GitHub legado**, commit `c92187031a204a4a01530f0b8662205c598b1e81`; não depende de worktree antiga no PC. A documentação dos megalotes está na **main do GitHub oficial**.

**Protegido fora do Git:** `local.properties` privado na fonte oficial e backup em `C:\Users\danil\AppData\Local\DanPlus\Protected_2_1_0`; certificado Shield `C:\Users\danil\AppData\Local\DanPlus\Shield\Signing\danplus-shield-release.p12`; certificado Fire `C:\Users\danil\.android\debug.keystore`. O certificado Fire foi comparado ao APK 2.0.2 com resultado PASS. Shield: alias/senha ainda NÃO TESTADOS para build futuro.

**Espaço medido:** C: 55,88 GB → 56,77 GB livres; D: 130,09 GB → 193,43 GB livres (**+63,34 GB**); projetos independentes preservados. O relatório completo de limpeza está em `D:\APK\DanPlus_2_0_2_HOMOLOGADO\FECHAMENTO_LIMPEZA_DANPLUS_20261119.md`.

**Próxima etapa ainda não realizada:** DP-ML00, sem gerar APK 2.1.0; a numeração 2.1.0 é decisão de produto, não build concluído. Não executar comandos históricos que usem caminhos das worktrees excluídas.
