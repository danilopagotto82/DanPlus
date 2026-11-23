# DANPLUS 2.1.0 — PLANO ÚNICO, SEM COMPILAÇÃO INTERMEDIÁRIA
**Data:** 22/11/2026 · **Status:** planejamento; NÃO autoriza modificar aplicativo, compilar, instalar ou publicar.

## 1. Base e fontes
Esta consolidação complementa, **sem substituir ou apagar**, os documentos `DANPLUS_2_1_0_PLANO_MESTRE_ATUALIZADO_20261122.md` e `DANPLUS_2_1_0_ROTEIRO_EXECUCAO_COM_SCRIPTS_20261122.md`, sua matriz `DANPLUS_2_1_0_MATRIZ_146_DECISOES.csv` e a auditoria RSQ referenciada `DANPLUS_2_1_0_AUDITORIA_RESEQ_SENIOR_20261122.md`. A matriz CSV e o relatório RSQ individual **não foram materializados nesta rodada**, embora seus resumos tenham sido lidos nas fontes. Recuperá-los na fase E00; não inventar os detalhes ausentes.

Decisões originais: **146 IDs**, sendo **105 APROVAR, 23 ADAPTAR, 8 INVESTIGAR, 4 MANTER, 5 NEGAR, 1 NÃO RECOMENDAR**. Há vínculos históricos que não são patches independentes. Demandas extras não alteram a contagem: REQ16/MUD, DP21-BRAND-01, DP21-LARGE-01, DP21-UPSTREAM-CI-01, DP21-BACKDROP-VITRINE-01, DP21-GRID-META-01, DP21-PAUSE-CREDITS-01, BUG-BACKDROP-01, LOGO-PTBR-01, BUG-THUMB-01, RS-01..07 e RSQ-01..20. Não contar 20 RSQ como 20 funcionalidades novas.

Baseline **documental**: DanPlus 2.0.2/1644 homologada, pasta `D:\APK\DanPlus_2_0_2_HOMOLOGADO`, fonte de referência `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`, repositório `https://github.com/danilopagotto82/DanPlus`, commit `main` observado anteriormente `3715ee0de8345d617f48c2da7271716ed501dc5c`. O binário 2.0.2 tem referência histórica `c92187031a204a4a01530f0b8662205c598b1e81` no repo OTA legado. **NÃO presumir que estas referências ainda equivalham ao estado do PC**; revalidar no E00. Não usar worktrees antigas.

## 2. Regra de execução — uma codificação, uma rodada final de build
1. **E00 em leitura primeiro:** confirmar AGENTS.md/GEMINI.md, Git, worktrees e alterações existentes; confrontar cada ID com código e binário. Classificação obrigatória: `JÁ EXISTE / PARCIAL / AUSENTE / NÃO APLICÁVEL / INVESTIGAR / DUPLICADO` e apontamento de arquivo/contrato/prova.
2. **Uma única worktree isolada, um único escritor Kotlin/Compose (Codex) por vez.** Gemini e ChatGPT são revisores, não editores concorrentes. Arquivo tocado em mais de um requisito pertence a um único megalote.
3. **Zero compilação durante E00–E09/E11.** Proibido `gradlew assemble`, `bundle`, `compile`, `test`, `lint` via Gradle ou qualquer ação que compile/gere APK/AAB; sem builds QA/debug, sem instalação, sem workflow automático que compile. **Testes de runtime permanecem NÃO TESTADOS** até E10. Permitidos apenas leitura, revisão de diff, `git diff --check` e análise estática comprovadamente sem compilação.
4. Antes de editar: autorização para o lote, proteção do 2.0.2, checkpoint reversível, branch nova e ausência de executor concorrente. Nenhum `git clean`, `reset --hard`, `force push`, stash geral ou alteração de dados de produção.
5. Fechar contratos e interfaces entre subsistemas antes da UI. Cada megalote entrega um patch integrado, testes **escritos mas não executados**, revisão estática e checkpoint. Nada de gerar APK para “ver como ficou” entre lotes.
6. **FREEZE DE CÓDIGO:** todas as funcionalidades elegíveis codificadas, dependências resolvidas, revisões completas, matriz sem IDs aprovados sem decisão, zero segredos no diff. Em seguida **E10 faz UMA rodada planejada de Gradle** que contemple as variantes reais Fire/Shield e os testes executáveis. Se falhar, corrigir e recompilar o necessário; não se pode prometer matematicamente uma única tentativa bem-sucedida.
7. Após compilar: smoke físico nos dois aparelhos, screenshots reais vs. homologado e verificação OTA/assinatura. Publicação/release/instalação e mudanças Supabase exigem gates próprios; uma build PASS não prova qualidade visual.

## 3. Separação por IA e executor
| Executor | Responsabilidade principal | Limites |
|---|---|---|
| **ChatGPT normal · GPT-6 · Alto** | Arquitetura, matriz única, triagem de conflito, decisão dos gates, crítica visual e revisão de resultados | Não acompanha jobs mecânicos; não edita Kotlin |
| **Gemini / Antigravity · modelo rápido de leitura** | Inventário extenso de código, strings, riscos de tradução, comparação ReSeq/Original/ARC, auditoria visual em screenshots existentes | SOMENTE LEITURA; achados com caminhos, IDs e provas; não alterar arquivo |
| **Codex · melhor modelo de programação disponível · Alto** | **Único autor** de Kotlin/Compose, player, integração, testes, recursos afetados e automação E11 | Não executar Gradle, gerar APK, fazer deploy ou alterar main até freeze/gate |
| **Git/PowerShell/terminal** | Checkpoint, diffs, SHA, verificações estáticas; após freeze, Gradle/ADB | Mecânico, sem agentes caros |
| **GitHub Actions** | Futuro monitor determinístico de upstream | Configurar desativado e sem build automático até aprovação posterior |

**Não é necessário Commander para esta fase de planejamento**. E00 remoto pode utilizar GitHub/arquivos do projeto; o estado local deverá ser confirmado na fase de execução por ferramenta autorizada no PC.

## 4. Sequência técnica consolidada, sem reabrir o mesmo código
| Ordem | Dono exclusivo | Escopo e aceite antes do freeze |
|---|---|---|
| **E00** | GPT-6 + Gemini | Inventário de 146 IDs, RS/REQ/MUD/RSQ, matriz de diferenças, check de signing/OTA, fallback YouTube sanitizado, libmpv, apps Fire/Shield, atualizações upstream. Apenas leitura. |
| **E01** (condicional) | Codex | Crash Compose, ANR, falha crítica de navegação e risco de vazamento de credenciais em redirect Plex/Emby **se comprovados**. Não alterar visual sem necessidade. |
| **E02** | Codex | Dono da resolução/failover de fontes: tentar todas elegíveis sem loop infinito, apenas uma ativa, cancelamento, timeout global, retry adaptativo, manter minuto/episódio/perfil/fonte manual. |
| **E04** após E02 | Codex | Dono de ranges/buffer/cache/memória: 64 MB quando comprovadamente adequado, seek, reentrada, Fire 32-bit, limites de RAM; arquivo grande sem downgrade REMUX. |
| **E03** após E04 | Codex | Dono do decoder/áudio/frequência: Exo/MPV, passthrough TrueHD/DTS-HD/DTS:X/Atmos, Dolby Vision/HDR, AAC5.1, HLS/DASH, AV1/VC-1, 59,94/60Hz. **Preservar Exo para áudio HD e não transcoder.** |
| **E06** | Codex | Contratos e dados estáveis de séries/episódios/Trakt/Continue Watching/Calendário, status, datas, ratings e contagem de episódios. Sem editar a UI. |
| **E07B-LÓGICA** após E06, antes de E05 | Codex | Scout/Descubra-me: filtros, surpresa, misturas, Play/Remix, episódio correto, PIN infantil e fontes; legendas/perfis/menus funcionais. **Congelar contratos de UI.** |
| **E05** (único lote visual) | Codex, Gemini revisa | Home/Vitrine/Glass, Focus Dock, carregamento/caching da arte, logos PT-BR, backdrops Indicações/Descobrir/Biblioteca, calendário premium, Seekr sem recorte, Grade lateral só séries, Pausa somente se problema real, DPAD. Nenhum segundo loader. |
| **E07A** | Gemini inventaria, Codex altera | Exibir marca DanPlus em UI, PT-BR, preservar créditos Nuvio/ARC, licenças e IDs técnicos; nada de replace global. |
| **E08** | Codex LAB + GPT revisa | Relatórios, indicações, Silo, Supabase, backup, RLS, consentimento e isolamento de conta/perfil. **Sem migration/deploy em produção.** |
| **E09** | Codex | Trailers dublados/legendados PT-BR, fallback original quando apropriado, cache TTL, YouTube sanitizado, Minority Report como teste; sem chave no código. |
| **E11** | GPT planeja; Codex escreve pipeline desativado | Ledger anti-duplicidade para Nuvio Original + ARC + ReSeq, checagem por SHA/patch-id, issues/PR draft futuros; **schedule e jobs build desabilitados**. |
| **FREEZE → E10** | GPT revisa; terminal/Gradle/ADB executam | Revisão integral, testes, compilação de variantes finais, signing, versionCode superior a 1644, 2 APKs quando exigidos pelos flavors reais, smoke Fire/Shield, OTA e screenshots. **Não publicar automaticamente.** |

**Justificativa da ordem revisada:** o roteiro antigo previa telas E05 antes do motor de recomendação E07B. Para evitar reabrir `ScoutScreen` e Focus Dock, **E06 entrega modelos → E07B entrega lógica/ações → E05 faz a ÚNICA passagem visual**. Se E07B precisar de novos elementos, a especificação completa deve estar congelada antes do E05.

## 5. Melhorias específicas do chat anterior
**Grade com prévia lateral** (`DP21-GRID-META-01`): E06 só fornece dados; E05 exibe para séries `🗓 2000– · 📺 120 episódios · 🛡 16 anos`, com badge discreto Em exibição/Finalizada/Cancelada quando comprovado. Excluir especiais da contagem regular salvo regra explícita; não confundir temporadas com episódios; não inferir classificação brasileira de TV-MA; não pedir TMDB a cada foco DPAD. Filmes não recebem episódio nem badge de status de série. Verificar em E00 se a superfície é o Focus Dock já existente ou outro componente. Quebra em 2 linhas se faltar espaço; sinopse sempre visível.

**“Você está assistindo”** (`DP21-PAUSE-CREDITS-01`): mostrar só Direção e Elenco, não Writer/Roteirista. O código público anteriormente analisado (`PauseOverlay.kt`/`PauseCredits.kt`) já parecia cumprir; **primeiro confirmar discrepância real** no APK e que roteiristas não estejam incorretamente agrupados como diretores. Não refazer por reflexo.

**FlickPick, Advanced Discover, TV Mix, Scout:** evoluir o **Scout existente** para “Descubra-me”, sem segundo motor, player ou seletor. Reaproveitar `surprise`, `ScoutEngine`, `ScoutViewModel`, `ScoutMixStore/Session`. Sugestões recentes por perfil, filtros por humor, gêneros, termos/tags TMDB (qualquer/todos/exatos), era, classificação infantil, não assistidos, listas salvas, Surpreenda-me/Outra opção e Play/Remix. Em séries, retomar episódio correto e respeitar ordem/tracking; preservar REMUX/seleção de fontes. Revisar possíveis repetições e UX do controle.

**Calendário:** manter aparência DanPlus (mosaicos, hero/backdrop, Lista/Semana/Mês, cinema/digital/Blu-ray), acrescentar somente gaps reais: Hoje, filtros por filme/série, lembrar vista/filtros, episódios seguintes, voltar à data escolhida, cache semanal. Sports/EPG/IPTV/Multiview ficam para **DAN TV+**, não portar ao DanPlus.

**Visual e artes:** identidade DanPlus premium escura/dourado discreto; preservar Vitrine/Classic/Glass, sinopse, foco DPAD. Um único caminho de carregamento de backdrops em Home, Indicações, Descobrir e Biblioteca; imagem deve aparecer no primeiro foco sem entrar em Detalhes. Logos TMDB **PT-BR primeiro, depois idioma original da obra** (não inglês arbitrariamente). Seekr mantém gerador RS-04; corrigir apenas moldura cortada. Comparar screenshots reais Fire/Shield após compilação final.

**Marca e codecs:** substituir somente marca visual ARC/Nuvio indevida por DanPlus; preservar nomes legítimos em `applicationId`/classes, license/NOTICE, créditos upstream, servidor, certificados e OTA. **REMUX sem transcodificar, reduzir bitrate/resolução ou sacrificar áudio HD/Atmos/DV.**

## 6. Exclusões e novidades externas
**Excluídos da implementação:** B1-29, B1-30, B1-31, B2-19, B2-20, A2-35. `RS-06` continua sem voto. 8 `INVESTIGAR` (B1-03/11/16/19/21, A2-08/11/32) e RS-02/07 **não liberam patch** automaticamente. 4 `MANTER` (B1-05/22/26, B2-04) são preservação/QA. B2-12/13 são histórico, sem reversão da função B2-08.

A auditoria ReSeq considera **RC3** (FlickPick/Advanced Discover/TV Mix) e **RC3.1** (tint escuro sutil e guia de TV); não portar guia de TV ao DanPlus. Original beta.3 e ARC.3 são propostas para **inspeção de diferença**, não autorização para copiar forks inteiros. ReSeq anuncia licença GPLv3: confirmar obrigações legais ao reutilizar código e manter atribuições.

**Riscos P0:** equivalência APK 2.0.2 vs fonte sanitizada (fallback YouTube), signing/certificados/OTA, libmpv dependência, redirect Plex/Emby com tokens, RAM Fire e áudio HD. Se o E00 detectar risco crítico, marcar HOLD e resolver contrato antes de continuar.

## 7. Entrega, evidências e gate
**FEITO nesta rodada:** análise de planejamento recuperado, confrontação de ReSeq RC3/RC3.1 em notas públicas, redistribuição por IA, regra zero builds e sequência anti-retrabalho, scripts prontos em arquivo companheiro.
**NÃO TESTADO/NÃO EXECUTADO:** PC e worktree atuais, matriz CSV integral/arquivo RSQ isolado, patches, Gradle, integração real, APK, screenshots, smoke Shield/Fire, OTA, ações GitHub, Supabase, CI ativo.

**Próximo passo EXATO:** executar **E00 somente leitura**, primeiro GitHub/arquivos existentes, depois inspeção local quando ferramenta do PC estiver disponível. Nunca interpretar estes documentos como autorização automática para programação, build, release ou deploy.
