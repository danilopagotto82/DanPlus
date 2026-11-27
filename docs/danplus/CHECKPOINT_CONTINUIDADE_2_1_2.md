# CHECKPOINT CONTINUIDADE 2.1.2

## ML01 — Design premium

- Branch `feat/danplus-2.1.2-homologacao-20261127`. Base `c27fbff4be8555aa6aa2e08b94cdfd8add0b62b3` e árvore `ea8e918e490a6b1ff9f56fdef884bc3206faa5e9` conferidas.
- Commit de implementação `529314c274b588c1cd2e6a64800892c0cead3f9f`. Árvore de implementação `276bf9183044686bdf9a1014b3306015a74f236b`.
- Teste focal `:app:testFullDebugUnitTest` em execução offline. Resultado final de JUnit — 111 PASS, 0 FAIL, 0 SKIP em 15 classes. XML em `app/build/test-results/testFullDebugUnitTest/`.
- `git diff --check` passou. A lista de tarefas foi conferida antes dos testes. O ML01 não gerou APK, não instalou, não publicou e não iniciou o ML02.
- ADB confirmou Shield com DanPlus 2.1.1/1646. AndroidTest e execução do candidato em TV continuam pendentes, assim como comparações antes/depois, DPAD físico, 1080p/4K, tempo de resposta, frames e RAM.

## Disposição por ID

- **UI-03 · PASS_TESTE_FOCADO.** Mesma faixa de 29% e grade. Altura medida depois de reservar metadados e créditos, até três retratos, criação e direção separadas, logo PT-BR/original com cache e cancelamento fora do foco. Testes — FolderPreviewCreditsTest; TmdbMetadataServiceTest; TmdbCollectionSourceResolverTest; GenreFoldersTest. Pendente — Capturar Ted Lasso antes/depois no mesmo perfil e resolução. Verificar retratos, textos de fallback, foco rápido, cache frio/quente e tempo de troca.
- **UI-04 · PASS_TESTE_FOCADO.** Enquadramento proporcional e degradê ancorado na borda real. Vitrine conserva 270 dp e ações. Sinopse medida em até duas linhas e selos de fundo escuro. Testes — CinematicPresentationTest; CollectionFolderCardMediaTest; CollectionLogoCropTest. Pendente — Capturar 20th Century Fox/Deadpool & Wolverine em arte clara e escura. Comparar rostos, junção à esquerda e legibilidade dos botões.
- **UI-11 · PASS_TESTE_FOCADO.** Painel de Descobrir e Biblioteca com 7 dp entre grupos, gêneros separados, nota e classificação com tratamento próprio. Recorte por medida de texto no dock, na vitrine e na grade lateral. Testes — MediaMetadataChipsTest; ScoutSynopsisTest; TraktBrowseIdentityTest. Pendente — Comparar fundos claros/escuros, títulos longos, escala de fonte e espaço útil da grade em Descobrir e Biblioteca.
- **RS-23 · PASS_TESTE_FOCADO.** Paleta escura adaptada ao tema e ao estilo, aplicada em Descubra-me, Mixes, calendário, painéis e menus afetados. Transparência relativa de 0 a 100 com piso de contraste. Cabeçalho preservado. Testes — CinematicPresentationTest; GlassMaterialTest; ClearGlassThemeTest; SignatureGlassTest; CalendarViewModelTest; ScoutReadyMixesTest. Pendente — Capturar Original, Liquid Dark e Cinematic Glass. Verificar 0/60/100%, tintas, cabeçalho, contraste e fluidez em Shield e Fire TV.
- **RS-24 · PASS_TESTE_FOCADO.** Prévia curta estática. Ler mais aparece quando há excedente e abre sob demanda o leitor rolável existente. Testes — ScoutSynopsisTest. Pendente — Testar DPAD no leitor, abertura somente por seleção, Back e restauração do foco, sinopses curtas/longas e ausência de rolagem fora da tela.

## Continuidade

Aguardar revisão do coordenador e a janela autorizada para comparação do candidato em TV. A matriz mantém a situação de todos os IDs dos megalotes posteriores.

## ML02 — Ações de título e avaliações públicas

- Base validada `acf45f79e4612e10cee28d703f1291c88aade51e`. ML01 aprovado tecnicamente pelo coordenador. Seus cinco registros foram preservados.
- Implementação `8128b1a183240f7b762916da1c8ef7d112bbf1cd`. Árvore `9542579795635c196e4d436e88924869c8a006ef`.
- `:app:testFullDebugUnitTest` offline passou com 91 testes, 0 falhas e 0 ignorados em 13 classes. XML em `app/build/test-results/testFullDebugUnitTest/`.
- `git diff --check` passou. Execução sem APK, instalação, publicação ou alterações em contas reais. AndroidTest e DPAD físico pendentes.

### Disposição por ID

- **UI-10 · PASS_TESTE_FOCADO.** Pressão longa oferece Indicar em Início, Continuar Assistindo, Descobrir, Busca, Biblioteca, coleções e catálogos aplicáveis. A ação abre o seletor MUD-006. Enviar requer seleção de destinatário e o acionamento explícito do botão. Metadados da mesma obra preservam as chaves de episódios. Testes — PosterRecommendationMetaTest, PosterOptionsControllerShowTest, ProfileRecommendationRulesTest, DanexusRecommendationsStoreTest, RecommendationSendReceiptTest. Pendente — Verificar DPAD, Back e retorno ao pôster em cada superfície. Fazer homologação de envio apenas com perfis sintéticos em ambiente autorizado.
- **UI-12A · PASS_TESTE_FOCADO.** A chamada da seção foi retirada. A aba Avaliações ficou no local atual, com as demais ações. O diff preserva os arquivos de armazenamento Trakt, de preferências e de notas do Descubra-me. Testes — TraktPersonalRatingRepositoryTest, TraktRatingEpisodeIdentityTest, ScoutRatingTest, ScoutRatingSyncLogicTest. Pendente — Verificar em TV um filme e uma série, inclusive navegação pelo trecho em que aparecia o botão.
- **UI-12B · PASS_TESTE_FOCADO.** TMDB confirmou tt7631058 ↔ 84773 nos dois sentidos e retornou HTTP 200 com oito episódios votados em cada T1–T3. SeriesGraph público retornou HTTP 200. O host de referência do Tapframe falhou no DNS da estação. Com as fontes configuradas, cada episódio recebe uma nota com a origem identificada. A aba distingue carregamento, ausência, erro e ocultação por preferência. Testes — ImdbEpisodeRatingsRepositoryTest, EpisodeRatingsIdentityTest, ImdbRatingVisibilityTest, EpisodeSynopsisUnderRowTest. Pendente — Homologar a aba e os cards em Shield/Fire com Os Anéis de Poder. Conferir T1–T3, DPAD, privacidade de notas ocultas, fonte, contraste e respostas lentas. Revalidar Tapframe se houver endpoint oficial configurado.

### Diagnóstico de provedores

- GETs de metadados públicos de Os Anéis de Poder confirmaram TMDB `84773` e IMDb `tt7631058`. As T1–T3 tiveram 8/8 episódios votados no TMDB. O SeriesGraph público retornou 8/8 na T1 e T2 e 6/8 na T3. O host de referência Tapframe ficou sem resposta HTTP nesta estação por falha DNS. As duas URLs parceiras estão vazias no `FullDebug`. [API do TMDB](https://developer.themoviedb.org/reference/tv-season-details) · [Resposta pública do SeriesGraph](https://seriesgraph.com/api/shows/84773/season-ratings)
- Com a chave TMDB existente, a alternativa oficial consulta uma temporada por vez. Apenas médias com votos e episódios correspondentes entram no fallback. Os valores mantêm a fonte por episódio, sem média entre provedores. Notas sem escala identificada vindas do addon mantêm rótulo e cor neutra. A leitura Trakt de avaliações pessoais segue reservada à implementação anterior.

## ML03 — Calendário Premium

- Base `5cb4f2d9f9af6ffcee077d0385cf7bd9f9909d7d` e implementação ML02 `8128b1a183240f7b762916da1c8ef7d112bbf1cd` conferidas. Matriz com 34 IDs. Somente CAL-01, CAL-02, CAL-03 e RS-21 foram atualizados. Os oito registros anteriores com PASS_TESTE_FOCADO foram preservados.
- Implementação `e6fa9d48c808718683727888736ecadf72973d60`. Árvore `fefac741f7093794eb0c4ea5ae23438a4a013ad8`. Os commits do ML03 foram registrados na cópia gravável `ml03-local`, com a mesma branch e o checkpoint oficial como pai. A worktree oficial em `D:` permaneceu no checkpoint de entrada por limite de escrita da sessão.
- A compilação direta com Kotlin 2.3.0 usou Compose, serialização e dependências locais. JUnit 4.13.2 executou 213 testes em 14 classes, sem falhas. `git diff --check` passou.
- A tentativa de `:app:testFullDebugUnitTest --dry-run` falhou ao se conectar ao daemon Gradle em loopback. O teste via Gradle, o processamento final de recursos Android e o AndroidTest continuam não testados.

### Disposição por ID

- **CAL-01 · PASS_TESTE_FOCADO.** Filtro comum a Lista, Semana e Mês. Formato de estreia e anime exigem dados identificados. Modo e filtro por perfil, data ao alternar ou voltar de detalhes e estado vazio preservado. Pendente — Testar DPAD, retorno ao card e troca de perfis em Shield e Fire TV.
- **CAL-02 · PASS_TESTE_FOCADO.** Reprodução com identidades fixas. Preserva datas publicadas e cache em falha. Distingue erro, carregamento e ausência. Retentativa recupera o título sem recarregar títulos válidos. Histórico e progresso pelas integrações existentes. Pendente — Confirmar casos do catálogo real com os mesmos IDs em Shield e Fire TV. Verificar virada local de dia e mês e atualização após falha.
- **CAL-03 · NÃO TESTADO.** A referência de código é ReSeq RC5.1 em 0470da8ea8ca04523fe60828bc22b28ac2dbadff. Seletor compacto, leitura por pôsteres, logos PT-BR no foco e retorno à data. Grade e mosaicos DanPlus presentes. Pendente — Capturar antes e depois em 1080p e 4K quando disponível. Medir foco, legibilidade, troca rápida de cartões, frames, RAM e cache frio/quente em Shield e Fire TV.
- **RS-21 · NÃO TESTADO.** CAL-01 e CAL-02 com PASS_TESTE_FOCADO. CAL-03 com auditoria de fonte e homologação visual e física a executar. Pendente — Fechar a disposição agregada após homologar CAL-03.

### Referência e continuidade

- `CalendarScreen` e `CalendarPreferences` orientaram o retorno à data e o ajuste por perfil. `CalendarViews` e `CalendarDetailPreview` orientaram o seletor compacto e a prioridade para pôsteres e arte. O mosaico, o atalho Hoje e as interfaces de lembretes continuam no calendário DanPlus. [CalendarScreen](https://github.com/abbaskip/Nuvio-ReSeq/blob/0470da8ea8ca04523fe60828bc22b28ac2dbadff/app/src/main/java/com/nuvio/tv/livetv/calendar/CalendarScreen.kt#L116) · [CalendarPreferences](https://github.com/abbaskip/Nuvio-ReSeq/blob/0470da8ea8ca04523fe60828bc22b28ac2dbadff/app/src/main/java/com/nuvio/tv/livetv/calendar/CalendarPreferences.kt#L24) · [CalendarViews](https://github.com/abbaskip/Nuvio-ReSeq/blob/0470da8ea8ca04523fe60828bc22b28ac2dbadff/app/src/main/java/com/nuvio/tv/livetv/calendar/CalendarViews.kt#L45) · [CalendarDetailPreview](https://github.com/abbaskip/Nuvio-ReSeq/blob/0470da8ea8ca04523fe60828bc22b28ac2dbadff/app/src/main/java/com/nuvio/tv/livetv/calendar/CalendarDetailPreview.kt#L26)
- A homologação visual e física depende de execução real em Shield e Fire TV. Esta rodada usou perfis e provedores simulados nos testes e não gerou APK, não instalou, não publicou e não alterou contas reais.
- O próximo megalote oficial é ML04, com PLAYER-01, PLAYER-02, RS-22 e RS-33. Aguardar autorização do coordenador para iniciar.

## ML03-SYNC + ML04 — Player inteligente

- Worktree oficial `D:/APK/Worktrees/DanPlus_2_1_2_Homologacao_20261127` preservada em `5cb4f2d9f9af6ffcee077d0385cf7bd9f9909d7d`. A sessão permite leitura em D:. Os arquivos rastreados foram conferidos. `docs/danplus/PLANO_DANPLUS_2_1_2.md` continua fora do índice.
- Bundle original do ML03 conferido por SHA-256 e `git bundle verify`. Cadeia linear `5cb4f2d9f9af6ffcee077d0385cf7bd9f9909d7d` → `e6fa9d48c808718683727888736ecadf72973d60` → `9af9df995767601d0f7ad574c374cc4d521dc27a` → `55c007f5fd9256ddc72e12a2b2fdffdd573d9115`. A implementação ML04 tem árvore `50d4bacc1ee6bd63d7ecba6c90dfca496982797a`. O trabalho seguiu na cópia gravável `ml03-local` na branch `feat/danplus-2.1.2-homologacao-20261127`.
- Matriz com 34 IDs. Somente PLAYER-01, PLAYER-02, RS-22 e RS-33 mudaram em relação ao checkpoint ML03. Os resultados anteriores foram preservados. Os 213 testes do ML03 não foram repetidos.
- JUnit 336 testes em 26 classes e compilação Kotlin/Compose do código alterado. AAPT2 compilou os quatro XMLs de recursos alterados. Sem falhas. `git diff --check` passou.
- JDK Temurin 17.0.21.1 e distribuição Gradle 8.13 conferidos. `--no-daemon`, com argumentos de JVM vazios e com argumentos do cliente alinhados ao projeto, ainda tentou iniciar JVM de uso único. A conexão TCP local falhou também no teste independente com `WinError 10013`. Caches e arquivos de dependências locais estavam legíveis. O Gradle completo e AndroidTest permanecem não testados.

### Disposição por ID

- **PLAYER-01 · PASS_TESTE_FOCADO.** MINIATURAS entre REDE e SISTEMA. Estado, origem quando disponível, cache com origem e intervalo quando apurado. Estado de reprodução e formatos MPV disponíveis no painel. Texto de origem usa mascaramento e categorias de servidor. Pendente — Conferir ordem, legibilidade e estados reais em Shield e Fire TV para Seekr, geração local, cache, servidor e falha.
- **PLAYER-02 · PASS_TESTE_FOCADO.** Seletor usa a trilha da barra com identidade e duração compatíveis. Até quatro capítulos visíveis, foco primeiro, pedidos canceláveis em série. Cache local existente e quadros já recebidos em pausa. Se faltar imagem, mantém rótulo, tempo e marcador visual. Seek exato existente para selecionar, avançar ou voltar. Pendente — A falha física de Interestelar segue sem contraprova. Reproduzir a edição real e verificar DPAD, seleção, Back, troca de episódio, retomada e buffering em Shield e Fire TV.
- **RS-22 · PASS_TESTE_FOCADO.** Somente Seekr persiste com os valores legados preservados, sem gerador local nem pré-carga local de episódios. Reutiliza SDK e cache existentes, prioriza reprodução durante buffering. Auditoria MPV recomenda ADIAR e mantém o AAR homologado. Pendente — Gates MUD-011 e MUD-013 seguem vigentes. Homologar Seekr com edição autorizada. Qualificar eventual 0.2.2 em LAB separado, incluindo as ABIs e codecs do gate.
- **RS-33 · PASS_TESTE_FOCADO.** A sessão cancela seus leitores de disco, acompanha encerramento da cópia e rejeita publicação tardia de chunks e bitmaps do servidor. Consulta de capítulos cancelada ao trocar ou sair do título. Preservados limites e perfis do REMUX e a cobertura 4K já autorizada. Pendente — Medir heap Java/nativo, memória de gráficos, chunks e capacidade de retomada em ciclos de abrir, buscar, trocar e fechar, especialmente no Fire TV ARMv7. Período prolongado em REMUX permanece não testado.

### MPV 0.2.2 — ADIAR

- DanPlus usa `io.github.abdallahmehiz:mpv-android-lib:0.1.12`, com libmpv `v0.41.0-174-g76a5eba99` verificado na ABI ARM64 do AAR em cache. O AAR inclui as duas ABIs do candidato. O POM declara Kotlin stdlib 2.0.21 e AppCompat 1.7.0. O leitor nativo carrega FFmpeg por símbolos e valida os majors `avcodec 62`, `avutil 60` e `swscale 9`. [Artefato atual](https://central.sonatype.com/artifact/io.github.abdallahmehiz/mpv-android-lib/0.1.12)
- ReSeq RC4 declara `mpv-android-lib:0.2.2` e informa ajuste do leitor ao FFmpeg fornecido. O repositório original confirmou releases até `v0.2.1` na consulta. O artefato 0.2.2 e um changelog reproduzível dessa versão ficaram sem confirmação direta nesta sessão. [ReSeq RC4](https://github.com/abbaskip/Nuvio-ReSeq/releases/tag/v1.0.0-rc.4) · [Dependência declarada](https://github.com/abbaskip/Nuvio-ReSeq/blob/855be1d1d1dd86a45b1131c5f48213ec7d34c553/app/build.gradle.kts#L607-L610) · [Releases do produtor](https://github.com/abdallahmehiz/mpv-android/releases)
- LAB proposto após obter fonte e AAR verificáveis. Fixar versão e SHA fora do candidato. Comparar empacotamento, dependências nativas, JNI e símbolos FFmpeg em ARM64 e ARMv7. Usar a versão atual como controle e conservar o bundle para retorno. Exercitar Dolby Vision, HDR, AFR, VC-1, passthrough, TrueHD, DTS-HD, DTS:X, AAC, ASS, seek e retomada. Adiar integração até resultados físicos e gate do coordenador.

### Referências e continuidade

- API e SDK Seekr 0.2.0, RC4/RC4.1 do ReSeq e comparação RC1 com beta 10.12 foram auditados. A comparação de memória do RC1 inclui testes de alocador nativo e alterações no AAR do Exo. Esta rodada limitou a mudança de memória aos ciclos de vida reproduzíveis das miniaturas e capítulos. [Seekr SDK](https://github.com/AKhalil609/seekr-android-sdk/blob/v0.2.0/seekr-android/src/main/kotlin/tv/seekr/previews/android/SeekrTrack.kt) · [RC4.1](https://github.com/abbaskip/Nuvio-ReSeq/releases/tag/v1.0.0-rc.4.1) · [Comparação beta 10.12 a RC1](https://github.com/abbaskip/Nuvio-ReSeq/compare/v0.1.0-beta.10.12...v1.0.0-rc.1)
- Shield e Fire TV permanecem pendentes para reprodução, DPAD, retomada, medições de memória e proteção do REMUX. Nenhum APK, instalação, publicação ou operação com conta real foi executado.
- O próximo megalote oficial é ML05, Histórico, trailers e Descubra-me (RS-25, RS-26, RS-28, RS-34 e RS-35). ML05 permanece por iniciar.

## ML05-S — Surpreenda-me

- Subentrega parcial na cópia gravável `danplus-ml05`, com base `37591d3` e implementação `6b921c6`. A worktree oficial permanece no checkpoint `5cb4f2d`.
- [Matriz ML05-S](MATRIZ_COMPLEMENTAR_ML05_S.csv) e [arquitetura, evidências e pendências](ML05_S_SURPREENDA_ME.md). Os 34 IDs da matriz principal foram preservados.
- Kotlin/Compose focal, 229 XMLs AAPT2 e 48 JUnit sem falhas na execução final. Gradle completo e AndroidTest não executados. Homologação visual e física em Shield e Fire TV **NÃO TESTADO**.
- Concluir as pendências de ML05/ML05-S. ML06 — OTA, organização e manutenção — permanece por iniciar.
