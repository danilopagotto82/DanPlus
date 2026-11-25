# CHECKPOINT E09 — DanPlus 2.1.0 | Trailers

## Base e escopo

- Branch: `feat/danplus-2.1.0-integracao-20261125`
- Worktree: `D:/APK/Worktrees/DanPlus_2_1_0_E02_20261125`
- HEAD inicial local e remoto: `ee27040daef377134d6c4a0ba3c0659a8e1af45e`
- Main remota: `04e5b4e1780c43f6de4e5627453985821c07d48b`
- Main local: `e67459cfb1042fe2e68f97b0a017f1411512bffe`
- Worktree limpa no precheck. A base contém os 14 commits E02, E02-R1, E04, E03, E03-R1, E03B, E06, E06-R1, E07B, E07B-R1, E05, E05-R1, E07A e E08, nessa ordem.

Foram consultados AGENTS.md, plano canônico, matriz 146, evidências 166, checkpoints relacionados e os quatro arquivos do [PR #1](https://github.com/danilopagotto82/DanPlus/pull/1), HEAD `f7d84cae2693d08e4678e28769d62182c55d5009`. Os contratos B3-D08 e B4-D17 também foram conferidos no [PR #2](https://github.com/danilopagotto82/DanPlus/pull/2), commit documental `5ede65ac39c23a4b36f8a98458860cc6b1713506`. Treze referências upstream retornaram na origem NuvioMedia/NuvioTV. `253eafc6d4de8` retornou HTTP 422 por SHA ausente, zero resultados na busca por hash e ausência de objeto Git local.

## Implementação

| ID | Entrega e evidência estática |
|---|---|
| REQ16-14 e DP-ML08 | Seletor compartilhado em `TrailerService.kt` e `TrailerPreviewLookup.kt`. Para TMDB/YouTube, trailers oficiais com indicação explícita de dublagem PT-BR precedem os legendados PT-BR e o idioma original informado pelo TMDB. PT-BR requer região Brasil nos metadados ou indicação textual inequívoca. O fallback por idioma configurado permanece e trailers precedem teasers. Um máximo de três candidatos reserva espaço para variantes diferentes. IMDb continua primário quando escolhido nas configurações. |
| A2-45 | Reuso de `ImdbTrailerResolver` para a mesma obra após recusa, ausência ou pausa do YouTube. Preservada a configuração de fonte. Recusa de acesso, limite de requisições e ID inválido não disparam resolução alternativa do mesmo vídeo. Falha de transporte pode usar o resolvedor existente, sem cache negativo definitivo. |
| RSQ-19, trailers | Áudio separado continua combinado via Media3. O seletor do trailer desabilita texto por padrão. Não havia configuração independente de legenda de trailer nesta base. O seletor e as preferências do filme principal ficam fora da alteração. |
| B3-D08 | `TrailerPlayerPool.kt` e `TrailerPlayer.kt` usam uma única propriedade de reprodução e troca de superfície Media3. A posição é reaproveitada para o mesmo par vídeo/áudio e a mesma seleção de perfil, com janela volátil de 30 segundos quando a mídia foi liberada. Player principal mantém o pool cedido até a devolução explícita. |
| B4-D17 | Primeiro frame acompanha a fonte, a superfície e o proprietário atuais. Pausa sem parada mantém a mídia preparada. `ON_STOP` libera mídia. Detalhes cobre o trailer pai enquanto Similares ou outra sobreposição toma a tela e pausa durante comentários e opções de reprodução. |

Detalhes exigia uma lista exclusivamente TMDB PT-BR, enquanto Home e Coleções já resolviam a identidade IMDb/TMDB, selecionavam provedor e testavam os IDs informados pela obra. Foi reaproveitado o fallback do PR #1 por meio do fluxo comum, incluindo bloqueio quando trailers estão desativados. Nas variantes de reprodução externa, Detalhes usa a mesma ordenação de TMDB e a saída externa existente. Os dois títulos continuam dependentes da disponibilidade real em cada dispositivo e região. [Minority Report, IMDb](https://www.imdb.com/title/tt0181689) · [A.I. Artificial Intelligence, IMDbPro](https://pro.imdb.com/title/tt0212720/)

Home, Coleções e Detalhes cancelam trabalho obsoleto ao sair ou mudar de entrada. A revisão identificou que PostPlay fazia busca de trailer durante o prefetch da recomendação do filme em andamento. Sua consulta de trailer agora depende de fim da reprodução e é cancelada ao mudar de seleção ou se a reprodução volta. A preparação de recomendações e avaliações conserva seu fluxo.

## Cache local e Supabase LAB

- Reutilizadas as chaves por obra, tipo, fonte, idioma e política de seleção, os locks de deduplicação e os limites do cache de URLs voláteis. O cache por vídeo YouTube também usa LRU de 128 entradas. A janela de recusa de reprodução é de dois minutos e o miss definitivo do serviço local dura 30 segundos. URLs vencidas ou que falharam têm a reutilização bloqueada.
- O buffer existente e o aquecimento IMDb limitado do E04 foram preservados. A implementação E09 não cria outro cache de mídia.
- `supabase/tests/e09_trailer_metadata_cache.sql` contém o contrato de IDs públicos, evidência, validade e RLS. `e09_trailer_metadata_cache_rollback.sql` é o rollback isolado. Chave composta cobre cinco dimensões. Somente serviço escreve. Leitura autenticada admite apenas registros vigentes. TTL máximo de seis horas para acertos e dois minutos para ausência. Ausências transitórias ficam fora do contrato de escrita. O produtor futuro deve validar a fonte, remover entradas invalidadas pelo provedor e limpar expiradas.
- O contrato está fora de migrations e roda em PGlite local com a suíte `e09-trailer-metadata.mjs`. Nenhuma conexão com o app ou com Supabase hospedado, nenhuma credencial e nenhuma nova API ou integração comercial. Produção e autorização dos provedores requerem decisão separada. [Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security) · [Políticas de desenvolvedor YouTube](https://developers.google.com/youtube/terms/developer-policies)

## Verificações

| Escopo | Resultado | Evidência |
|---|---|---|
| Git, arquivos no escopo e revisão estática | PASS | Base, refs, histórico e diff conferidos. `git diff --check` e `git diff --cached --check` limpos. |
| Sintaxe Kotlin | PASS | PSI Kotlin 2.3.0 processou os 20 arquivos Kotlin do patch com zero erros sintáticos. A análise não compila o app. |
| Contrato LAB | PASS | `node --check` e PGlite 0.5.16. Chave de cinco dimensões, IDs inválidos, janela negativa, expiração, rejeição de escrita autenticada e de leitura anônima, invalidação por serviço e rollback. |
| Regressões Android escritas | NÃO TESTADO | Fixtures dos dois filmes, identidade sem ambiguidade de tipo, PT-BR dublado e legendado, limites e fontes recusadas, opt-out, cache frio/quente e falhas temporárias, ON_RESUME, troca de proprietário, perfil A → B → A e cessão ao player principal. Casos existentes de foco rápido, indisponibilidade e deduplicação permaneceram. |
| Dispositivo e backend hospedado | NÃO TESTADO | Reprodução efetiva dos dois filmes, disponibilidade regional e de incorporação, primeiro frame em navegação, áudio e legendas, RAM, interação com diálogos, isolamento visual e comportamento do PostPlay. |

### Roteiro de validação física para autorização posterior

1. Em Shield e Fire TV, selecionar cada um dos dois filmes na Home e numa pasta. Abrir Detalhes, voltar e abrir Similares. Checar que a fonte corresponde à obra, o primeiro frame aparece e o mesmo trailer mantém posição quando elegível.
2. Trocar rapidamente de obra, abrir um diálogo, alternar perfis A → B → A, ir ao segundo plano e voltar. Checar chamadas canceladas, exclusividade de áudio, legendas padrão desligadas e opt-out.
3. Testar dois fluxos de áudio, dublagem, legendagem integrada ao clipe e trailer original. Forçar vídeo indisponível, restrição regional, perda de rede e timeout. Conferir fallback da mesma obra, cache frio, cache quente e recuperação.
4. Abrir o filme principal após trailer. Conferir cessão de codecs, ausência de buscas de trailer até o fim da reprodução e busca da seleção atual no PostPlay. Comparar RAM e comportamento de foco e arte de Signature Glass com o checkpoint anterior.

Sem Gradle, build, APK, instalação, push, merge ou release. MPV 0.1.12, aplicação, assinatura, OTA, perfis homologados e migrações preservados. Auditoria independente antes do push. E10 e E11 não iniciados.
