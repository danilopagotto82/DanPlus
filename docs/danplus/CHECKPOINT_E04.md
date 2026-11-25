# Checkpoint E04 — DanPlus 2.1.0

25/11/2026 · Implementação candidata · Homologação no FREEZE/E10.

## Precheck

- Repositório `danilopagotto82/DanPlus`, branch `feat/danplus-2.1.0-integracao-20261125` e worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`.
- HEAD local e remoto iniciais `0e82f59675a84d2e3c3ca425f16aac1b74ffc4b4`. Worktree limpa no precheck. E02 `af4b063` e E02-R1 `0e82f59` integram a base protegida.
- Checkpoint E02, AGENTS.md, plano, matrizes 146/166 e [PR documental #2](https://github.com/danilopagotto82/DanPlus/pull/2) em `5ede65ac39c23a4b36f8a98458860cc6b1713506` consultados. Patches upstream inspecionados em `2cd82e7889a0`, `362b911407f7`, `0845ea3b7e60` e `1ffb58f37182`.

## Reconciliação e mudanças

| IDs | Evidência e intervenção |
| --- | --- |
| B1-22 | Política térmica e frequência de verificação preservadas. |
| A1-23, A2-27 | O orçamento adaptativo e os tetos por ABI já existiam. Tarefas enfileiradas de pressão e empréstimo, aviso de pouca memória e limpeza após seek passaram a verificar o desligamento do controlador antes de atuar. O teto de 32 bits continua ligado ao processo real. |
| A2-02, A2-26 | O seek do usuário antes podia ser consumido por uma reabertura curta ou por outro endereço. O marcador agora se restringe ao stream e é consumido numa reabertura fora da janela. O descarte de um resultado interrompe também a tarefa de download no executor, tanto no caminho principal quanto na tentativa escalada. Reservas e chunks da janela de destino foram preservados. |
| A2-21, A2-25 | Dimensionamento, preparação do diretório e abertura do índice VOD deixam a construção síncrona do MediaSource. Uma abertura fria segue direto até fechar. Reaberturas podem usar o cache pronto, com verificação de espaço, limites existentes e offsets `Long`. Um diretório antigo que não pode ser separado deixa a reprodução direta durante o processo, evitando limpeza concorrente no diretório ativo. |
| A2-29 | O teste de formato ignora o cache persistente de VOD, mesmo com a opção de cache ligada. |
| B3-D01 | O fitting do back-buffer e as reservas já existiam. A proposta de orçamento para DV7 só é aplicada quando a conversão teve sucesso, o orçamento é gerenciado, a memória é baixa e o novo valor reduz o vigente. Reflexão, JNI e política de formato seguem no escopo original. |
| B3-D02, vínculos B4-D15, A1-28, A2-34/A2-36/A2-39 | As dimensões de cards já distinguem pôster compacto e arte expandida. Os três caminhos de prefetch passam a usar a revisão do pôster que o card consulta. A fila vertical inclui os extras de fallback na identidade. Revisões antigas têm metadados limitados a 512 entradas. Se uma revisão sai do mapa, a chave zero anterior deixa de ser reutilizada para a imagem invalidada. Variantes, resolução, hardware bitmap e layout permanecem com suas políticas existentes. |
| RSQ-05 | Varredura oportunista e retry de catálogos, atualização automática da Home por TTL e sincronização em lote dos canais do launcher passam a respeitar a sessão do player. Persistência, histórico, scrobble, progresso, atualização manual e recuperação E02 mantêm seus caminhos. Trabalho iniciado anteriormente pode concluir. |

## Arquivos principais

- Player e memória — `BufferMemoryKeeper.kt`, `BufferSizing.kt`, `ParallelRangeDataSource.kt`, `RangeSeekHint.kt`, `PlayerMediaSourceFactory.kt`, `VodCacheWhenReadyDataSource.kt` e `PlayerRuntimeControllerInitialization.kt`.
- Imagens e Home — `HomePosterLoading.kt`, `ImageInvalidationBus.kt`, `RememberImageRevalidationKey.kt`, `ModernHomeRows.kt`, `ModernHomeRowsList.kt`, `ProgressiveHomePosterWarmup.kt`, `HomeViewModel.kt` e `HomeViewModelCatalogPipeline.kt`.
- Launcher — `AndroidTvChannelSyncService.kt`.

## Revisão estática

**PASS estático** — escopo Git, revisão integral do diff, `git diff --check` e análise sintática dos arquivos Kotlin alterados. A análise sintática deixa a resolução de tipos para o build de E10.

## Testes de regressão preparados

**NÃO TESTADO.** Suítes novas ou ampliadas, com execução no E10:

- `BufferMemoryKeeperTest` e `BufferSizingTest` — pressão enfileirada e controlador removido, tetos por ABI e redução de orçamento DV7 com e sem conversão.
- `ParallelRangeSeekDropTest` — avanço/retrocesso, dois seeks em sequência, isolamento da fonte, expiração e cancelamento da tarefa.
- `VodCacheWhenReadyDataSourceTest` — cache frio e pronto, reabertura em 5 GiB, leitura direta em ByteBuffer, listeners, armazenamento pequeno e bypass de teste de formato.
- `HomePosterLoadingTest` — paridade de chaves frias e revalidadas, tamanho e descarte de metadados.
- `PlayerSessionTrackerTest` — atividade opcional aguarda a saída mesmo com a reprodução já estabilizada.

## Build e smoke

**NÃO TESTADO** — Gradle, build e instalação reservados ao E10. Sem APK, push, merge, release, alteração da main, assinatura, applicationIds, OTA, produção ou Silo remoto. E03 permanece pendente. Preservados os caminhos E02/E02-R1, StartCushion, ReadAheadReserve, Seekr, perfis, áudio passthrough, HDR, DV e AFR.

## Limites e próxima ação

- Uma abertura fria contínua pode terminar sem usar cache em disco quando não houver reabertura. O custo de escrita quando o cache fica ativo, o tempo de abertura e a efetividade do cancelamento durante a leitura de um corpo real precisam de medição.
- No E10, rodar também as regressões E02/E02-R1. Medir RAM/PSS, tempo até a primeira imagem, rebuffer, cache, seek para frente e para trás e trabalho de Home/launcher na NVIDIA Shield Pro 2017 e na Fire TV Stick 4K Max de 2ª geração. Verificar ABI do processo, DV7 convertido/nativo, REMUX grande, Seekr e perfis REMUX, Fire, FHD, DUBLADO, ANIME e INFANTIL.
- Próximo passo — o coordenador audita o commit local a partir de `0e82f59` e decide sobre push e autorização de E03.
