# DANPLUS · DP-CODEX-07 — CHECKPOINT DE CONTINUIDADE DA PREPARAÇÃO
Data de execução da organização: 17/11/2026. STATUS: pacote DOCUMENTAL preparado e conferido, sem mexer no aplicativo.

## Objetivo e escopo
Handoff de megalotes ao Codex: corrigir P0 Detalhes, depois UX/miniaturas, histórico, player/fonte, legenda, relatórios/backend com gates, trailers separados e OTA 2.0.1 isolada. O registro atual é **planejamento**, não teste/execução de patches.

## Ferramentas e rota econômica
Coordenador ChatGPT GPT-6 raciocínio Alto + Remote Desktop Commander (documentação e leitura). Executor futuro Codex com modelo de código mais capaz disponível e raciocínio Alto, trabalhando diretamente no Windows e em worktree NOVA. Commander/PowerShell/Gradle para testes e builds mecânicos; sem Work, sem agentes caros para monitorar processos.

## Baseline e Git
- PC DESKTOP-5P89BAD, worktree homologada D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115.
- Branch danplus/2.0-ml01-integracao, HEAD 0ce90a0740656c903b9780d2d423966b15119697, git status **somente ?? .wrangler/** (preexistente); nenhum arquivo de código editado nesta preparação.
- Central documental pós-merge PR1 HEAD 99f740cd63e73cb8f5f3b6a51180a4bd001b4d7e; a tree de código deve ser confirmada pelo Codex antes de criar nova worktree.
- **Trilha OTA já suja:** D:\APK\Worktrees\DanPlus_PONTE_OTA_MLC1_20261117, branch danplus/mlc1-ponte-ota-20261117, HEAD 99f740c; status atual: modificações app/build.gradle.kts, AbiSelector.kt, UpdateRepository.kt, ForkUpdateRepositoryTest.kt e diretório app/src/testFullFire/ não rastreado. PRESERVAR, NÃO RESETAR.
- Repositório atual GitHub danilopagotto82/DANPLUS-Shield-Arc31-Private, central branch danplus/2.0-centralizacao-20261117. Repositório futuro escolhido para PLANEJAMENTO danilopagotto82/DanPlus; não criar/renomear/publicar nesta preparação.
- Versão entregue 2.0.0/code1642; ponte futura 2.0.1, nome exibido DanPlus; applicationIds/certificados distintos Shield/Fire precisam ser mantidos.

## Homologações e decisões
- R3 MUDs: 14 conjuntos (001..013, incluindo 007A/007B) em grande parte codificados na 2.0.0. Código não prova visual/hardware; preservar Vitrine, Focus Dock, Navegação, Episódios Classic/V2, relógio e perfis.
- Auditoria upstream original: 146 IDs, 36 decisões (ADAPTAR/MANTER/INVESTIGAR/NÃO RECOMENDAR); 110 pendentes. B2-06 PENDENTE.
- Reshaped: 7 IDs, RS-01 ADAPTAR, RS-02 INVESTIGAR, RS-03 ADAPTAR, RS-04 MANTER, RS-05 ADAPTAR, RS-07 INVESTIGAR; RS-06 PENDENTE.
- BUG-THUMB-01: corte inferior do quadro de miniaturas no screenshot do proprietário, causas possíveis ContentScale.Crop, proporção fixa 16:9, ThumbCrop e clip/âncora. Corrigir apresentação futuramente, preservar motor existente.
- Trilha OTA ML-C1 possui 14 testes PASS e build Fire local PASS registrados historicamente, mas a worktree mudou depois do relatório; build Shield FAIL por keystore não localizada. Não declarar o estado posterior testado.

## Etapas concluídas de fato
- Dispositivo e Git do PC consultados, documentação R3/P0/ML-C1/RS lida em extensão suficiente para a síntese. Leitura integral de 14 pastas DECISAO/MISSAO/SCRIPT originais nesta rodada: **NÃO EXECUTADA**, tarefa obrigatória DP-ML00.
- Matriz com 146 IDs e sete RS criada e verificada em D:\APK\Organizacao DanPlus 2.0\DP_CODEX_03_MATRIZ_146_E_RS_20261117.md.
- Arquivos DP_CODEX_00..06 criados e relidos, com plano dos megalotes DP-ML00..10 e script mestre. Fontes originais não sobrescritas.
- Nenhuma branch Git/commit/código/app/APK/OTA/instalação/SQL/Supabase real criada ou alterada nesta missão.

## Testes e evidências
- PRECHECK Commander/Git: PASS quanto à existência da base protegida e estado observado.
- Integridade da documentação por leitura de retorno: PASS.
- Correções, testes Kotlin, build, Lint, smoke físico Shield/Fire, screenshot do aplicativo após patch: **NÃO EXECUTADOS / NÃO TESTADOS**.
- Riscos principais: crash P0 de nó Compose na tela Detalhes, Fire 32-bit, ponte OTA DIRTY, assinatura Shield, segredos/alertas API, RLS do backend, regressão de áudio/legenda, conflito de provedores PT-BR/pt-PT, referências históricas pendentes de releitura.

## Proibido nesta preparação e no Codex sem gate
Não mexer nas worktrees protegidas/dirty, .wrangler, PIN/segredos, main, tag, release, OTA real, assinaturas, Supabase produção, aparelhos ADB, backups existentes. Não implementar 110 IDs pendentes, RS-06 ou B2-06; não fazer patch a partir de INVESTIGAR sem prova e decisão.

## Pesquisa adicional solicitada após preparação dos megalotes
- Fork Reshaped possui YAML de verificação cron a cada 2h, auto-merge condicionante e build/teste/assinatura, porém schedule recente não confirmado; workflow está em branch subtitle-autosync e o default atual é animeIntegration; últimos runs agendados AutoSync identificados em 09/10 e releases 17/11 foram `workflow_dispatch`.
- Estudo em `DP_CODEX_08_ESTUDO_AUTOMACAO_UPSTREAM_20261117.md`. Plano futuro **DP-ML11 DA-00..DA-04** somente após gate, começando por monitor e relatório só leitura; nenhum workflow do DanPlus criado nem CI/merge executado.

## Próxima ação exata
Abra Codex com acesso ao D: do Windows, modelo de código mais capaz e raciocínio Alto. Cole o conteúdo de DP_CODEX_06_SCRIPT_INICIO_20261117.md. Execute DP-ML00 somente leitura, valide Git e os 14 MUDs canônicos. Se PASS, crie NOVA worktree isolada para DP-ML01 e trate exclusivamente crash P0 da tela Detalhes, com testes e evidência, sem publicação ou instalação. Após ML01, reportar e solicitar gate para os lotes seguintes.

## ADENDO DE CONTINUIDADE — 18/11/2026 — ML11 MONITOR DUPLO
Decisão do proprietário: o monitor deve vigiar **Nuvio Original (NuvioMedia/NuvioTV) e Nuvio ARC (ysosrs123/NuvioTV-Fork) obrigatoriamente**. ARC é a linhagem histórica DanPlus; a baseline real para comparar comportamento é a worktree DanPlus protegida. Reshaped é apenas complementar/opcional.
Evidências nesta retomada: Commander online DESKTOP-5P89BAD; base protegida HEAD 0ce90a0 com somente ?? .wrangler/; ponte OTA HEAD 99f740c com quatro arquivos rastreados modificados e app/src/testFullFire/ não rastreado. Nenhum deles foi alterado.
Documentos alterados fora do Git de aplicativo: DP_CODEX_00, DP_CODEX_01 e DP_CODEX_08; novos DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md e DP_CODEX_10_SCRIPT_ML11_MONITOR_DUAL_20261118.md. Matriz 146/36/110, RS-06 e B2-06 permanecem intactos. Nenhum monitor, CI, Action, repo, script executável, commit, build APK ou deploy foi criado. Etapa DA-00..DA-04: NÃO IMPLEMENTADO/NÃO TESTADO.
Próxima ação de automação: usar o roteiro DP_CODEX_10 SOMENTE em missão futura com autorização para implementação local segura; ML00/ML01 de estabilidade continuam no DP_CODEX_06 e não devem ser misturados.
