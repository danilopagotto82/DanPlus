# Checkpoint E07B — DanPlus 2.1.0

25/11/2026 · Revisão estática · Auditoria independente pendente.

## Base e evidências

- Worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, branch `feat/danplus-2.1.0-integracao-20261125`. HEAD local e remoto no precheck `73954cd8e7fd7d52c994e589cd052a9206f9dd49`. Main local `e67459cfb1042fe2e68f97b0a017f1411512bffe`, main remota `04e5b4e1780c43f6de4e5627453985821c07d48b`.
- Aplicados AGENTS.md e as linhas E07B da matriz 146 e evidências 166. Checkpoints E02, E02-R1, E04, E03, E03-R1, E03B, E06 e E06-R1 consultados para preservar os contratos. Plano complementar explicitamente fixado em `5ede65ac39c23a4b36f8a98458860cc6b1713506`. [PR #2](https://github.com/danilopagotto82/DanPlus/pull/2)
- Upstream inspecionado por commit. ASS/SSA: [233dd1f3b03a](https://github.com/NuvioMedia/NuvioTV/commit/233dd1f3b03a), [8ce69c2a1787](https://github.com/NuvioMedia/NuvioTV/commit/8ce69c2a1787). Rede e parsing: [80b1b33db5e9](https://github.com/NuvioMedia/NuvioTV/commit/80b1b33db5e9), [2ed752298654](https://github.com/NuvioMedia/NuvioTV/commit/2ed752298654), [2cba17e43c41](https://github.com/NuvioMedia/NuvioTV/commit/2cba17e43c41). AutoSync/áudio: [2a2c4849f311](https://github.com/NuvioMedia/NuvioTV/commit/2a2c4849f311), [2defc5246b45](https://github.com/NuvioMedia/NuvioTV/commit/2defc5246b45), [c3762be9ffea](https://github.com/NuvioMedia/NuvioTV/commit/c3762be9ffea), [1935dc4cfa6f](https://github.com/NuvioMedia/NuvioTV/commit/1935dc4cfa6f).

## Gaps, decisões e contratos

| IDs | Resultado no E07B |
| --- | --- |
| A1-07/33/34/35, A2-41, A1-42, RSQ-01 | O Surpreenda-me ignorava o filtro de assistidos e a linha cacheada podia reaparecer em outra sugestão. A seleção usa as últimas 20 sugestões exibidas no perfil, prefere títulos novos e exclui a linha atual. Se o conjunto elegível acabar, usa as sugestões menos recentes disponíveis. Adulto, família e grupo passam pelo limite mais restritivo aplicável, com PG no modo família e rejeição de classificação desconhecida sob limite. Entradas cacheadas e a seção baseada em histórico mantêm o filtro. |
| RSQ-02 | O Scout expõe `ScoutAdvancedSearch`, `applyAdvancedSearch`, `saveAdvancedSearch`, `forgetAdvancedSearch` e `discoveryHistory` com o perfil associado. Reaproveita a busca e o Discover TMDB com termos inteiros normalizados, gêneros, ano, idioma original e keywords AND/OR. As consultas são limitadas a três páginas por seleção e 20 buscas salvas por perfil. Quando a API só oferece Discover, o termo exato filtra os resultados dessas páginas. Idioma original é metadado e a disponibilidade de dublagem segue a seleção de fontes. |
| A1-30, RSQ-03 | Reaproveitados armazenamento por perfil, personalização, Remix, reprodução e fila aleatória dos Mixes. `ScoutMixViewModel.preview()` entrega a fila preparada sem iniciar reprodução ou gravar histórico. Prévia e disparos são bloqueados após troca de perfil. |
| A1-32, A2-47 | Preservada a identificação de recap de filme de anime com idioma/origem e marca explícita de resumo. Títulos ambíguos e obras próprias ficam disponíveis. Reconciliados apenas anime, Scout e perfis deste lote. |
| A1-38, B2-15, A1-39 | Validação de PIN do grupo aceita a resposta apenas na seleção que a solicitou. A troca invalida a lista de membros e desbloqueios locais. O editor de perfis mantém o contrato existente de PIN. O seletor de visibilidade do Descubra-me no menu é por perfil e preserva o acesso à rota existente. |
| B1-08/09/10/17/28, B2-01/06/17, A1-02/13/14, B4-D12 | Preservados fonte importada TTF/OTF, contorno, sombras 1–8, idiomas, posição, capítulos, legendas de servidor e escala PGS. O contrato `AssSubtitleStyleSettings` persiste por perfil separado do texto comum. Libass usa escala ASS de 100% por padrão. MPV mantém o estilo autoral por padrão e a personalização depende de opt-in, com fonte importada e posição próprias. `PlaybackSettingsViewModel.setAssSubtitleStyle(profileId, style)` fica disponível ao editor E05. Renderização e faixas pretas exigem smoke no E10. |
| B4-D11, B3-D09 | A sondagem embutida usa `PlayerPlaybackNetworking.playbackCallFactory` do E02-R1. HTTP 403/5xx, Range incompatível, corpo truncado, cancelamento e timeout deixam a sondagem indeterminada, sem gravar ausência. Preservados escopo, TLS/ALPN, Range, cabeçalhos, cancelamento, retry único e orçamento próprio da sondagem. Reconhecidos `S_ASS` e `S_SSA`. A normalização CR e o parsing após seções de fontes já existiam e recebem regressão. |
| B4-D10, RS-01, RS-03 | AutoSync V2 já prepara atividade/janelas e exige score ≥ 0,90, margem ≥ 0,04 e três segmentos para atalho, com validação estrutural e fallback. Perfil ambíguo conserva o sincronismo original. SDH usado na análise não é removido pelo retimer do arquivo exibido. Ganho de velocidade requer benchmark. Política E02 de fontes preservada. |
| B3-D10, B4-D14, RSQ-19 | Cache de normalização de idiomas limitado a 256 entradas. Nome autoral da trilha tem precedência e ASS/SSA recebe identificação. A seleção da faixa permanece no contrato existente de navegação. `KEYCODE_MEDIA_AUDIO_TRACK` abre o seletor quando disponível e respeita mapeamentos explícitos e diálogos inclusive Blu-ray. PT-BR/PT-PT e DTS preservados. |

### VAD, ASR e libass

- **B3-D11**. A adaptação upstream de áudio depende de decodificação/tap PCM, runtime e modelo. Um caminho leve local exige amostragem PCM com cancelamento, orçamento e verificação do passthrough no Fire/Shield. VAD fica condicionado a esse gate. ASR permanece investigação. A sincronização baseada em áudio permanece indisponível por padrão, sem downloads, novos modelos, dependências ou envio de áudio.
- **B4-D13**. O upstream [1a18710234a1](https://github.com/NuvioMedia/NuvioTV/commit/1a18710234a1) substitui o AAR pelo blob `12b1203281e64688ffaae08720f0ed44fce58907`. O AAR local preservado tem SHA-256 `4D3A51B7FD4BDF94399BBCF7CC1EE9B231A0F0B72725772D9F5EFE0FF717C029` e ABIs arm64-v8a, armeabi-v7a, x86 e x86_64. A inspeção do `classes.jar` confirmou a API de escala necessária. A licença precisa ser vinculada ao artefato exato de 0.0.14, com proveniência, SHA-256 publicado, conteúdo nativo, ABI, benchmark e validação do coordenador antes de substituição. MPV permanece em 0.1.12.

### Adjacências e handoff E05

- **B2-08/B2-12/B2-13**. Configuração de ambiente examinada. Estado de teclado físico/IME permanece para smoke. Histórico de reversões preservado. **B1-32/A1-12** seguem no E07A. **A2-52** preserva o identificador Silo existente e reserva o selo visual ao E05.
- E05 recebe o modelo e as ações de busca e histórico no Scout, a prévia de Mixes, a visibilidade por perfil, a aparência ASS por perfil e os nomes/formatos das trilhas. Editor visual, foco, layout e os novos controles de filtros ficam para E05.

## Verificações e matriz E10

- **PASS estático**. `git diff --check`, parser PSI dos 32 arquivos Kotlin, XML dos dois recursos e revisão de escopo/ancestralidade. Os 17 casos novos incluem histórico/repetição, busca e persistência por perfil, família, assistidos, anime, Mix, ASS/SRT/PGS, CR/SDH, HTTP e cancelamento. Preservadas as regressões anteriores.
- **FAIL**. Nenhuma falha nas verificações finais executadas. **NÃO TESTADO**. JUnit, resolução de tipos por build, instalação, UI/dispositivo e benchmarks.
- **E10**. Fazer Scout com perfil INFANTIL, grupo com PIN, títulos sem classificação e perfil ANIME. Trocar perfil durante a busca/PIN, conferir buscas e histórico após reabrir, conferir filtros de assistidos e não assistidos, ativar/remover entrada de menu e testar Mixes. Conferir upload TTF/OTF por celular, ASS autoral e personalizado, SRT e PGS, ajuste HDR, cancelamento e 403/500 reais no índice, temporização curta/ambígua e SDH exibido. Conferir seletor de áudio, memória com muitas trilhas, retorno de extra, fallback E02, seek/cache E04, AFR E03 e recibos/autorização do E06-R1.
- Gradle, build, APK, instalação, push, merge e release não executados. Main, dependências, binários, perfis homologados, IDs, assinatura, OTA e produção preservados. E05, E07A, E08 e E09 seguem fora deste lote.

Próxima ação — auditar o commit E07B via Commander acima de `73954cd`, com foco no isolamento do Scout, no opt-in ASS e na sondagem HTTP/2. Aguardar liberação explícita antes do push.
