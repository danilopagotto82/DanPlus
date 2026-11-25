# Checkpoint E02 — DanPlus 2.1.0

25/11/2026 · Implementação candidata em branch de integração · Homologação pendente.

## Precheck

- Repositório `danilopagotto82/DanPlus`, branch `feat/danplus-2.1.0-integracao-20261125`, HEAD inicial `04e5b4e1780c43f6de4e5627453985821c07d48b`.
- Worktree dedicada em `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`. Branch remota e base local conferidas. Worktrees preexistentes limpas no precheck e preservadas.
- Plano, matrizes 146/166, anexo B1-27 e decisões Reshaped consultados nos refs oficiais. C01 reaproveitado como precheck sem compilação. As fontes oficiais incluem o [PR documental #2](https://github.com/danilopagotto82/DanPlus/pull/2), HEAD `5ede65ac39c23a4b36f8a98458860cc6b1713506`, e os patches upstream `44549e9a0dbe`, `f4ce0aa640c3`, `4b71d71aed0f`, `2cba17e43c41` e `2f58e48e5827`.

## Entrega e rastreabilidade

| Requisitos | Implementação candidata |
| --- | --- |
| B1-27, B2-07, A1-20, A1-21, A2-04, A2-15, A2-49, RS-03 | Failover sequencial dentro do grupo de addon atual e do mesmo pedido/perfil. Percorre a lista após a fonte atual e volta ao início uma vez por varredura. Mantém os filtros de elegibilidade, REMUX, dublagem e regex existentes. Deduplica URLs normalizadas, candidatos Debrid e versões de servidor. Usa velocidade passiva disponível apenas para desempatar opções equivalentes, sem sondar as fontes. Prazo único de 120 s para a cadeia de troca, com cancelamento da seleção, da preparação e do próximo start ao expirar. |
| B4-D01, A2-10 no escopo de recuperação | READY com tunneling continua liberando o player, mas não encerra o direito a failover de partida. Renderização ou progresso estável confirma a reprodução. VC-1 conserva retry de decoder, fallback interno para MPV e ação manual quando aplicável. |
| B4-D02, B2-02, A2-16 | Troca manual e automática carregam posição atual ou carregada entre tentativas. Seek explícito tem precedência, inclusive quando vale zero. A duração conhecida limita o ponto inicial. ExoPlayer e MPV recebem a posição de partida. Mudança de episódio descarta a posição antiga. |
| A1-10, A2-13 | Renovação já existente da fonte recusada continua restrita ao mesmo arquivo. Se ela falhar, o autoplay pode seguir para outra fonte elegível do mesmo addon. URLs expiradas ou repetidas são excluídas da cadeia. B1-16 continua em investigação. |
| B4-D03, vínculo B2-14 e A2-04 | Seletores Debrid aceitam nomes alternativos com `&` ou `and` após os nomes exatos. Mantêm o caminho do arquivo e a precedência do índice quando aplicável. Candidatos ambíguos ou com episódio explicitamente divergente são recusados. |
| B3-D09, vínculos A1-11 e B2-06 | Um retry HTTPS em HTTP/2 para HTTP/1.1 403 qualificado, na mesma origem da mídia com headers de playback. Preserva a requisição, inclusive Range e autenticação, fecha respostas, conserva o 403 definitivo e propaga o cancelamento do Media3 à chamada ativa. Retry sujeito a TLS validado e ALPN HTTP/2, sem redirecionamento ou desafio de autenticação. |
| B3-D15, vínculos B1-01 e RSQ-13 | Exclusão de repositório exige confirmação do alvo. Revalida identidade e perfil gravável e usa o escopo de perfil existente para remover. Reusa a apresentação de confirmação existente. Foco e apresentação próprios ficam no E05. |

Análise dirigida de B4-D18 não estabeleceu necessidade neste lote. A2-11 e B1-16 continuam em investigação. Os vínculos de catálogo não implicam conclusão de formato VC-1 no E03, correção geral de hashes de torrent, mudanças na detecção de cache TorBox ou política de rede de legendas no E07B. O retry de mesmo link preexistente continua com seu prazo local de pesquisa.

## Arquivos alterados

- Seleção e prazo — `SourceFailoverSelection.kt`, `SourceFailoverWindow.kt`, `PlayerRuntimeControllerErrorRecovery.kt` e `PlayerRuntimeControllerReusedLink.kt`.
- Integração com player e perfil — `PlayerRuntimeController.kt`, `PlayerRuntimeControllerEngineFailover.kt`, `PlayerRuntimeControllerInitialization.kt`, `PlayerRuntimeControllerLifecycle.kt`, `PlayerRuntimeControllerMpv.kt`, `PlayerRuntimeControllerMpvEvents.kt`, `PlayerRuntimeControllerObservers.kt`, `PlayerRuntimeControllerPlaybackEvents.kt`, `PlayerRuntimeControllerServer.kt`, `PlayerRuntimeControllerStreams.kt`, `PlayerStartupPlaybackPolicy.kt` e `PlayerViewModel.kt`.
- Transporte — `PlaybackHttp2RetryCallFactory.kt`, `PlayerPlaybackNetworking.kt` e `PlayerMediaSourceFactory.kt`.
- Debrid, plugins e mensagens — `DebridFileSelection.kt`, `PluginUiState.kt`, `PluginViewModel.kt`, `PluginScreen.kt` e `strings.xml` nos diretórios `values` e `values-pt-rBR`.

## Análise estática

**PASS estático** para revisão do diff, `git diff --check`, inspeção sintática Kotlin em comparação à base, XML e recursos adicionados. Inspeção do AAR local confirmou que o Media3 usa `Call.enqueue` e propaga `Call.cancel` quando o loader é interrompido. A análise sintática não resolve tipos nem executa Kotlin.

## Testes unitários

**NÃO TESTADO.** Suítes escritas ou ampliadas para rodar no E10:

- `SourceFailoverSelectionTest.kt` — 0/1/7 fontes, meio/fim de lista, fontes inválidas/expiradas, duplicatas, velocidade passiva, filtros, prazo, escopo e posição.
- `PlayerStartupPlaybackPolicyTest.kt` — READY tunneled presumido versus reprodução confirmada.
- `DebridFileSelectionTest.kt` — equivalência de nomes para os provedores suportados, caminhos, caixa, ambiguidade, índice e episódio.
- `PlaybackHttp2RetryCallFactoryTest.kt` — 403 → HTTP/2, 403 definitivo, origem, desafio, redirecionamento, Range, autenticação sintética e cancelamento.
- `PendingRepositoryRemovalTest.kt` — confirmação restrita ao alvo original e ao perfil gravável.

## Build

**NÃO TESTADO.** Gradle, testes automatizados, compilação, geração e assinatura de APK ficam para FREEZE/E10. Configuração de versão, manifests, assinaturas e OTA permanecem na base 2.0.2/1644. APKs homologados preservados.

## Smoke

**NÃO TESTADO.** Shield Pro 2017 e Fire TV Stick 4K Max de 2ª geração. No E10, executar trocas manual e automática nos dois motores, decoder VC-1/tunneling, lista parcial, cancelamento real durante preparação, troca de perfil/episódio, expiração em reprodução e negociação ALPN com servidor sintético. Verificar foco e confirmação de plugin, REMUX, Fire, FHD, DUBLADO, ANIME, INFANTIL, Seekr, miniaturas e áudio/vídeo homologados. Validar 120 s com fontes lentas e observar o custo de repetição em endpoints que recusam HTTP/1.1.

## Próxima decisão

Auditoria independente do commit E02 pelo ChatGPT coordenador, com revisão dos fluxos de expiração e cancelamento e da cobertura prevista no E10. A campanha de runtime depende de FREEZE/E10. E04 depende de decisão do coordenador. Nenhuma instalação, publicação de APK, release ou merge integra esta entrega.
