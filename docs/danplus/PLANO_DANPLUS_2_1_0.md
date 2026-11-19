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

## Segurança/qualidade e pontos de parada
- Próxima missão recomendada: **DP-ML00 somente leitura**, em GPT-6 Alto + Commander; Codex Alto só para patch Kotlin/Compose. Antes de começar, verificar `AGENTS.md` de D:\APK, branch, HEAD, working tree, processos concorrentes, dependências e evidências reais.
- Para codificar: branch/worktree NOVA criada da fonte oficial limpa. Preservar a main. Sem trocar applicationId, certificados, assinatura, URL OTA ou banco de produção.
- Assinatura Shield / certificado Fire, Gradle build e teste de fallback YouTube: ainda não homologados para **nova fonte sanitizada**.
- 2 alertas Google API Key históricos permanecem no repositório legado. O repositório oficial novo não tinha alertas abertos na última consulta; não importar histórico.
- Sete workflows herdados desativados; não reativar sem revisão.
- Critério de entrega: PRECHECK → PLANO → PATCH → TESTE FOCADO → BUILD → SMOKE FIRE/SHIELD → DIFF/SEGREDOS → gate de publicação. **Não dizer PASS para o que não foi testado.**

## Limpeza expressamente autorizada
O proprietário autorizou descartar **todas as cópias antigas locais do DanPlus**, inclusive branches com alterações não commitadas, por considerar suficiente a 2.0.2 publicada e o plano futuro. Preservar, porém, os itens mínimos de assinatura, fonte oficial, plano, APKs 2.0.2 e arquivos de outros projetos listados acima. Registrar os diretórios realmente excluídos e o espaço efetivamente recuperado, sem inventar sucesso.
