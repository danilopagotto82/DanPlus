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

## ML05-FINAL — continuação de f940474

- Implementação `fb38abbacfb45d703bfcee20afc06f101ec2f822`, árvore `2ad479b2aedd7728efcb7a8a3fc5d52e28cf60db`. Validação final Kotlin/Compose e JUnit 4.13.2 com 124 testes, 0 falhas, 16 classes. AAPT2 compilou os dois XMLs atualizados. Gradle completo e AndroidTest continuam NÃO TESTADOS. Nenhum APK foi gerado, instalado ou publicado.

- [Matriz principal](MATRIZ_EXECUCAO_2_1_2.csv) e [matriz complementar](MATRIZ_COMPLEMENTAR_ML05_S.csv) preservam 34 IDs canônicos e 10 IDs DP-S. As situações focais não representam aceite do fluxo em aparelho.
- Seu gosto reutiliza as palavras-chave dos metadados já coletados. Mostra apenas temas recorrentes em pelo menos dois títulos distintos com peso positivo. Sessões permitem convidado temporário, limite familiar e inclusão explícita de assistidos. Exclusões de progresso, biblioteca, rejeição e classificação ainda valem.
- A revisão BR usa a região do dispositivo, metadados TMDB e cache regional separado. Ausência de classificação brasileira resulta em título indisponível na sessão restrita. Filmes podem usar pré-filtro BR. Séries exigem checagem individual pela classificação da série no TMDB. [Guia do Ministério da Justiça](https://www.gov.br/mj/pt-br/assuntos/seus-direitos/classificacao-1/paginas-classificacao-indicativa/guia-de-classificacao)
- IA com ajuste de escopo. O ranking local passa por filtros e duração antes de consultar resultados já armazenados da ordenação opcional. Somente sessão individual pode promover um dos três finalistas. Sem nova requisição generativa, dados de convidados ou nova credencial. Trailer externo usa o resolvedor PT-BR e o aplicativo responsável pela URL. A volta conserva o filme escolhido.
- A auditoria encontrou o pai ausente `03095024919c431dd274e6e2bea8016066156a0c` referenciado pelo commit histórico `a00c26c045091e7ddfee91591ad14fdabe2315eb`. A transferência recuperável usa `5cb4f2d` como base. Preservar a fonte oficial e o bundle anterior, sem reparo automático.
- Pendências de implementação e diagnóstico. DP-S04 ainda precisa de cadastro de vínculo/faixa etária e onboarding dos demais perfis. A confirmação após reprodução precisaria de autorização por participante e de um contrato de atribuição no player. RS-26 ainda exige cobertura sintética integrada de histórico retroativo, progresso parcial, reconexão Trakt e rewatch. A validação física permanece no ML08.

### Preparação de ML06

- [Adendo UI-13 importado](ADENDO_ML06_UI13_20261127.md). ML06 deve planejar o backdrop atrás do cabeçalho, reaproveitar `behindTopNavigation()` e manter o inset dos controles. Tratar como complemento independente se ML06 já estiver fechado.
- A chave persistida nova é `cinema_favorite_genre_v1`, string de ID TMDB no DataStore Scout por perfil. A ausência representa qualquer gênero. A preferência pode ser removida pelo usuário e consta no catálogo atual de backup por perfil. Inventariar sua responsabilidade no ML06 para evitar gravação duplicada. Clima, duração, companhia, convidado e recusas da escolha ficam em memória. Classificação BR usa cache derivado, descartável e separado por região.
- Google Drive deve complementar Nuvio/ARC com responsabilidade exclusiva por configuração e reaproveitar o ReSeq após auditar compatibilidade. Configurações técnicas de avaliação ficam locais. Seekr deve entrar somente no backup criptografado.
- O Client ID WeTrakr foi transferido para o `local.dev.properties` ignorado pelo Git na cópia gravável. O valor foi comparado sem ser exibido. A configuração efetiva de compilação ainda depende do gate Gradle.

### Checklist físico para ML08

Usar perfis sintéticos ou autorizados. Registrar versão, resolução, variante, fonte de metadados, idioma, estados de foco e PASS/FAIL por cenário no Shield e no Fire TV. Situação desta missão em todos os casos **NÃO TESTADO**.

| Filme ou série | Caminho | Problema anterior ou cenário | Resultado esperado |
| --- | --- | --- | --- |
| Minority Report | Coleções → pôster em foco → Detalhes → Trailer → Back | Trailer podia reiniciar na transição. | Mesma obra e posição, prioridade PT-BR, foco devolvido, trocar foco cancela e Assistir cessa tráfego de prévia. |
| A.I. Inteligência Artificial | Surpreenda-me → Filtros avançados → termo exato → Escolha por mim → Trailer | Trailer externo ausente na escolha ou foco perdido ao voltar. | Variante externa abre o app responsável. Ao voltar, mesmo diálogo, filme e foco em Trailer. Outra escolha não repete título recusado. |
| Interestelar | Perfil adulto → Surpreenda-me → Minha sessão de cinema → 90 min | Escolha poderia ignorar limite ou renovar com o mesmo título. | Filme acima do limite fica fora. Ausência é informada sem relaxar filtros. No ML06/ML08, usar também o caso de backdrop escuro do UI-13. |
| Toy Story | Perfil infantil em região BR → Minha sessão de cinema → Companhia → incluir assistidos | Uso de rótulo estrangeiro ou exclusão sem opção de rever. | Classificação BR real aplicada conforme o limite. Reexibição só com opção explícita e regras de rejeição preservadas. |
| Deadpool & Wolverine | Perfil infantil BR → busca avançada → Surpreenda-me | Perfil infantil poderia receber obra sem classificação local conhecida. | Título acima do teto ou sem classificação BR fica fora. No ML06/ML08, conferir arte colorida contínua atrás do menu. |
| Ted Lasso | Mixes → Somente não assistidos → Play → Back → Detalhes → Episódios | Episódio concluído podia voltar à seleção ou afetar sequência. | Mix só usa episódios inéditos elegíveis. Back preserva foco. Sequência normal e histórico do perfil permanecem corretos. |
| Os Anéis de Poder | Continuar Assistindo → opções do episódio → marcar com data anterior → reconectar tracker | Progresso parcial e episódios concluídos podiam ser reconciliados incorretamente. | Data preservada, remoção limitada a progresso parcial e sem reativação de série concluída. Aplicar ao perfil autorizado e conferir Trakt/WeTrakr separadamente. |
| Duna: Parte Dois | Home clássica/moderna → Descobrir → Surpreenda-me, transparência 0/60/100 | Faixa entre cabeçalho e backdrop com arte clara. | Critério UI-13 no ML06/ML08. Arte contínua e relógio legível, sem duplicação de fundo ou recursos. |

O próximo megalote oficial é ML06, após tratar o que continua aberto no ML05-FINAL. Homologação física no ML08.

## ML05-CLOSE — aceite técnico focal

- Base `1eea456`; integração `b5a05eeb342c2a1593b3a42475b2eef3ff937bd2`, árvore `a2176897297b7c486cdc335d82d5c0796da1ef38`. RS-26, DP-S04 e DP-S10 implementados e registrados como **PASS_TESTE_FOCADO**. ML05 encerrado tecnicamente. ML06 é o próximo lote e pode partir deste checkpoint.
- **RS-26.** Continuar Assistindo aceita data e hora locais no formato indicado. A operação conserva o carimbo no progresso concluído, item assistido e eventos dos trackers. Uma marca preexistente com carimbo igual ou mais recente continua válida. A remoção exige progresso em curso e identidade de filme ou episódio. O filtro preserva entradas concluídas e entradas posteriores à seleção. O Trakt revalida o registro pausado, a identidade da conta e a autorização antes do DELETE. A rotina de histórico e a projeção WeTrakr foram exercitadas com dados sintéticos.
- **DP-S04.** O seletor reutiliza perfil, avatar e PIN. Vínculo e faixa etária são opcionais. Salvar ou pular encerra a oferta inicial. A edição continua no seletor. As faixas criança e adolescente só podem tornar o limite de cinema mais restritivo. Convidados continuam temporários.
- **DP-S10.** O convite para confirmar presença fica desativado por padrão. Ao ativá-lo, um filme elegível gera uma confirmação única ao sair após concluir. A correlação exige o ID TMDB ou seu alias IMDb já conhecido, o perfil e a geração atuais. Cancelamento, troca de perfil/conta e eventos duplicados invalidam a confirmação. O recibo fica no perfil anfitrião com os participantes explicitamente selecionados. O fluxo de presença não grava históricos de terceiros nem chama trackers.
- As chaves `cinema_relation_v1`, `cinema_age_band_v1`, `cinema_onboarding_seen_v1` e `cinema_last_attendance_v1` usam o DataStore privado `scout_cinema_private` por perfil. São dados locais com separação por perfil. O registro do último comparecimento é substituído a cada confirmação explícita. O catálogo de exportação existente não inclui esse armazenamento. Preservar essa responsabilidade na auditoria de backup do ML06.
- **Validação.** Kotlin/Compose direto em ambiente local, AAPT2 dos dois recursos adicionados, 92 testes JUnit em 11 classes, zero falhas, e `git diff --check`. A rodada inclui reconexão de teste no Trakt, filtro de rewatch, isolamento local, PIN, cancelamento e consumo único da presença, além das regressões WeTrakr. Gradle completo, AndroidTest e homologação física **NÃO TESTADOS**. Nenhum APK ou teste em conta de produção.

### Checklist físico complementar para ML08

Usar o mesmo caso no Shield e no Fire TV. Registrar resolução, fonte, perfil, foco antes/depois e resultado por aparelho. Situação de todos os casos **NÃO TESTADO**.

| Filme ou série | Caminho | Cenário anterior | Resultado esperado |
| --- | --- | --- | --- |
| Os Anéis de Poder | Continuar Assistindo → menu do episódio → Marcar como assistido → data anterior → reconectar Trakt | A data podia virar a hora atual e a remoção parcial podia incluir a série. | Data preservada. Remover somente a reprodução parcial do episódio, preservando episódios concluídos e rewatch mais recente. |
| Interestelar | Continuar Assistindo → remover um rewatch parcial → abrir Detalhes → conferir histórico → trocar perfil | O item parcial e a marca legítima poderiam ser confundidos. | A remoção conserva a conclusão e o histórico original. O outro perfil conserva seus próprios dados. |
| Toy Story | Surpreenda-me → Minha sessão de cinema → Companhia → perfil protegido → PIN → complementar ou pular | Não havia vínculo/faixa ou onboarding. | PIN errado ou cancelado não abre a ficha. Salvar ou pular dispensa a oferta seguinte. A faixa etária reforça o limite. O convidado continua temporário. |
| A.I. Inteligência Artificial | Surpreenda-me → Minha sessão de cinema → Companhia → ativar confirmação na escolha → Assistir → concluir | Não havia confirmação de presença. | Confirmar somente os selecionados, com gravação local no perfil anfitrião. Pular, sair cedo, trocar perfil/conta ou repetir o evento não grava presença. Uma reprodução sem adesão segue normalmente. |

## ML06 consolidado — continuidade de f65bc0b

- Implementação em 6b302948fc771ecea34e392e11b8d4c8d4a684ff, árvore 0280197b7f3ec5de887891b480e52bd3f5a225bc. Kotlin/Compose direto, quatro XMLs AAPT2, 62 testes em oito classes, zero falhas e diff check. Gradle completo, AndroidTest e homologação em TV **NÃO TESTADOS**. Nenhum APK de candidato produzido.
- **OTA-01 e RS-31.** O banner usa bytes da transferência compartilhada, total conhecido, andamento indeterminado, velocidade e cancelamento HTTP. Assinatura, tamanho e SHA-256 quando fornecido pelo release são conferidos antes da publicação. O descarte contabiliza bytes efetivamente excluídos e valida o caminho dentro do diretório privado. A reserva do instalador protege a cópia até a confirmação da versão instalada. Um arquivo reservado ainda sem confirmação permanece retido. [Assets GitHub](https://docs.github.com/en/rest/releases/assets)
- **RS-32.** Controles existentes organizados em Reprodução imediata, Autoplay temporizado e Fallback. Validação de foco, ordem de fontes e comportamento por aparelho permanece no ML08.
- **RS-30 — estudo concluído.** API 31 introduziu o controle de ação do usuário para sessões PackageInstaller. Versão do sistema, alvo do app, origem da instalação e permissões alteram a decisão. O instalador deve admitir STATUS_PENDING_USER_ACTION. Gate físico independente deve verificar autorização, assinatura, ociosidade, alimentação, espaço, ausência de reprodução, cancelamento e reinício. Downgrade e rollback dependem das políticas do aparelho e da preservação de dados. Uma versão corretiva assinada com versionCode superior é a opção quando rollback de sistema estiver indisponível. Nenhuma ativação silenciosa nova. [Android PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams)
- **RS-29 — transferido para ML06.** Backup manual local, telefone e seletor de documentos. Drive aparece quando há provedor compatível no aparelho. O formato existente sela Seekr com AES-GCM e PIN portátil e exclui a seção técnica do dispositivo das novas cópias portáteis. O protótipo automático admite unicamente cinema_favorite_genre_v1 e usa escopo de conta/perfil/dispositivo, base compartilhada, envelope autenticado e escrita condicionada à revisão. O transporte Drive foi ensaiado com interceptador HTTP. O ReSeq RC4 foi auditado. A verificação de revisão e reconciliação por base orientaram a adaptação. O exportador Live TV e o tratamento de credenciais de provedores permaneceram fora do DanPlus. OAuth, provisionamento de grant e arquivo, agendamento na abertura/troca de perfil e ativação no aplicativo continuam no gate separado. Usar debounce e cache da última revisão quando houver grant válido. [Fonte ReSeq](https://github.com/abbaskip/Nuvio-ReSeq/blob/v1.0.0-rc.4/app/src/main/java/com/nuvio/tv/livetv/sync/LiveTvDriveSync.kt) · [Licença GPLv3](https://github.com/abbaskip/Nuvio-ReSeq/blob/v1.0.0-rc.4/LICENSE) · [Seletor Android](https://developer.android.com/guide/topics/providers/document-provider)
- A [matriz de responsabilidade](MATRIZ_RESPONSABILIDADE_BACKUP_ML06.csv) registra autoridades existentes, dados manuais e a lacuna candidata ao Drive. O Client ID oficial WeTrakr tem precedência sobre a alternativa manual. Validar incorporação nas duas variantes no ML08.
- **UI-13.** Home clássica imersiva usa o modificador existente da Home moderna, Scout e atmosfera V2. O inset de 54 dp dos controles foi preservado. Composição visual, contraste e DPAD seguem no ML08. [Adendo UI-13](ADENDO_ML06_UI13_20261127.md)
- **Companhia.** Um pool utilizável ou gênero favorito declarado fornece afinidade existente. Perfis sem esses sinais e convidados passam pela seleção temporária TMDB com múltiplos gêneros ou sem preferência. Mínimo de 65% e média de 35% usam uma nota por pessoa. Cinco avaliações positivas produzem sementes e alteram a assinatura de cache. A ausência de pool confirmado ainda pode exigir a seleção rápida. O adendo fornecido foi incorporado. [Adendo Companhia](ADENDO_ML06_COMPANHIA_20261127.md)
- **ML07.** Os 34 IDs canônicos e os 10 complementares foram preservados. LAB-EM-CENA fica em HOLD nesta versão. O checkpoint recuperável serve de base para ML07, com o gate Drive e a homologação física em separado.

### Checklist físico ML08 — ML06

Executar em Shield e Fire TV. Registrar cenário, perfil, foco, resolução e resultado por aparelho. Situação **NÃO TESTADO** em todos os casos.

| Título ou cenário | Caminho | Resultado esperado |
| --- | --- | --- |
| Release OTA de teste | Início → banner → Baixar → Cancelar | Bytes, tamanho quando conhecido, velocidade e andamento indeterminado quando necessário. Só publicar pronto após validação. |
| Release pendente | Banner → Descartar download; repetir com instalador em andamento | Liberar somente os bytes selecionados; preservar instalação em curso. |
| Interestelar e Duna: Parte Dois | Home clássica e moderna → focar arte clara/escura → Descobrir | Backdrop da Home imersiva contínuo. Controle, relógio e contraste em 0/60/100% e retorno por Back. |
| Deadpool & Wolverine | Home, Descobrir e Surpreenda-me → trocar foco e arte | Transição contínua atrás do cabeçalho; logo separado e posição de relógio, avatar e engrenagem preservada. |
| Garota Exemplar e Diário de uma Paixão | Surpreenda-me → Minha sessão de cinema → Companhia → gêneros | Preferências temporárias individuais e resultado coerente com o grupo. |
| Toy Story | Minha sessão de cinema → convidado → gêneros e limite familiar | Preservar classificação, PIN e temporariedade do convidado. |
| Interestelar com dois perfis | Ajustes → Seleção de fontes → imediato, temporizado e fallback | As fontes permitidas e ordem seguem a configuração de cada perfil. Conferir REMUX aprovado no gate de reprodução. |
| Backup de teste | Backup e restauração → exportar → importar em outro aparelho | Segredos exigem PIN correto. Preferências técnicas ficam locais. Drive real somente no gate próprio. |

## ML07 — checkpoint de desenvolvimento para ML08

- Implementação e estudos em 9ddda14. Base ML06 em 8828596, com implementação em 6b30294. Branch feat/danplus-2.1.2-homologacao-20261127. HEAD oficial conferido em 5cb4f2d. Commits ML07 locais.
- 69 testes JVM em 10 classes, sem falhas. Kotlin/Compose direto com dependências e classes em cache. Gradle completo, AndroidTest, variantes e aparelhos **NÃO TESTADOS**. Nenhum APK, instalação ou publicação.
- RS-27/36 e LAB-USENET ficam como estudos. As fixtures de episódios são sintéticas e a seleção alternativa permanece desligada. Drive v2 permanece desativado no app, com três preferências portáteis e transporte simulado testado. HIST-01 conserva 194 registros documentais e segue pendente para reconciliação individual completa. A contagem da matriz canônica permanece 34, e a complementar mantém 10. LAB-EM-CENA permanece HOLD. [Estudos e limitações](ML07_ESTUDOS_E_GATES.md)
- JDK 17, plataforma SDK 36, AAPT2, distribuição Gradle 8.13 e cache de dependências presentes. A resolução integral de dependências das variantes precisa do ML08. O Client ID WeTrakr em local.dev.properties na cópia coincide com o da worktree oficial e está ignorado pelo Git. Verificar a injeção nas variantes no gate. Keystore e configuração de criptografia da assinatura release não foram encontrados no ambiente de build consultado. Providenciar por canal privado antes de release assinado. OAuth/Drive exige gate externo independente da compilação.
- Recuperar o bundle cumulativo ML07 em repositório isolado com a base 5cb4f2d9f9af6ffcee077d0385cf7bd9f9909d7d. Verificar pré-requisitos, SHA-256, fsck e árvore antes de preparar o ambiente de compilação. A cópia gravável já contém o HEAD novo. A worktree oficial deve ser inspecionada quanto a alterações locais e preservada. Usar checkout isolado do repositório recuperado se a oficial estiver ocupada ou se faltar objeto histórico. Reprovisionar a configuração privada para a árvore efetiva, mantendo local.dev.properties fora do Git.

### Complemento do checklist físico ML08

Executar as três tabelas de ML05/ML06 acima e os casos abaixo em Shield e Fire TV. Registrar variante, aparelho, caminho, foco, título e fonte. Todos os casos permanecem **NÃO TESTADOS**.

| Título ou cenário | Caminho | Problema anterior | Resultado esperado em Shield e Fire TV |
| --- | --- | --- | --- |
| Duna: Parte Dois e Interestelar | Home clássica → rolar da faixa hero para as fileiras → alternar foco → subir e voltar | Fundo do catálogo começava abaixo da barra | Continuidade atrás da barra e legibilidade com 0/60/100% de transparência. Relógio, avatar e foco mantêm posição. |
| Deadpool & Wolverine | Home moderna → Descobrir → Biblioteca → Para Você | Possível faixa ou arte mais escura nas transições | Fundo de página contínuo. Focus Dock permanece no painel. DPAD e Back restituem o foco. |
| Lost | Biblioteca → série → episódio → selecionar fonte → retomar | Reordenação poderia usar o número de outro episódio para tracking | Ordem Certa atual preservada. Retomada e histórico verificáveis usando perfil autorizado, sem ativar ordem experimental. |
| Minority Report | Detalhes → trailer externo → voltar | Possível perda de foco e escolha PT-BR inconsistente | Retorno ao diálogo ou detalhe de origem com foco preservado. Conferir prioridade de versão PT-BR. |
| Dois perfis com cópia fictícia | Configurações → Backup → exportar/importar → trocar perfil | Preferência de outro perfil pode parecer aplicada após retorno | Backup manual respeita escopo, PIN e preferências técnicas locais. Drive automático permanece desligado. |

## ML07-DEFINITIVO — continuidade para o gate ML08

- Base de partida e949c17. Worktree oficial conferida em 5cb4f2d e mantida com seus documentos locais. Cópia danplus-ml05 contém a árvore candidata. Não usar reset ou forçar a worktree oficial.
- Commits da execução: 488af06 OTA, e122a6c notas, 6bc6979 e cff9e4f foco Biblioteca, ef492b4 e 7c9fb78 preparação Drive, 0ba3c79 automação em modelos desativados. Os 13 itens e o complemento de notas têm disposição separada no relatório. [Resultados e gates](ML07_DEFINITIVO_RESULTADOS.md)
- 232 testes JVM focados em 28 classes, Kotlin/Compose direto, AAPT2 nos dois XMLs alterados e diff check aprovados. Gradle FullDebug compilou app e fontes AndroidTest. Instrumentação e homologação física permanecem NÃO TESTADAS. Os testes locais do Destaque Especial, das respostas ao usuário e do gateway de relatórios passaram em banco isolado. A automação passou em três testes determinísticos, actionlint e inventário público somente leitura.
- Gradle reconheceu e resolveu, em dry-run conjunto, :app:assembleFullRelease e :app:assembleFullFireFireOta para 2.1.2/1647 com danplusUnifiedOta. Nenhum APK candidato foi gerado, instalado ou publicado. Nas releases e tags consultadas em DanPlus e no repositório OTA legado, não apareceu 2.1.2 ou referência a 1647. A maior versão verificada nos APKs de referência foi 2.1.1/1646.
- Certificados e chaves originais encontrados e comparados, com correspondência SHA-256 para Shield e Fire TV. Shield de referência com.danplus.shield/arm64-v8a e Fire TV com.danexus.tv.fire.debug/armeabi-v7a. O nome do arquivo de keystore histórico do Fire funciona como configuração original desse pacote e mantém o certificado homologado. Não gerar outro. O Client ID WeTrakr coincide com o privado na worktree oficial e o arquivo permanece ignorado. A injeção efetiva e as bibliotecas nativas do candidato ficam no gate quando os APKs forem gerados.
- Gate final bloqueado em RS-29. Drive API ativa no projeto DanPlus Drive Test. Completar OAuth e consentimento com dados fictícios, provar escrita condicional e provisionamento em dois clientes e concluir integração segura com armazenamento e UI. A API simulada não basta para liberar o recurso. Verificar também pendências físicas do histórico e do player. [Checklist Shield e Fire TV](CHECKLIST_FISICO_ML08.md)
- HIST-01 classifica os 194 registros com evidência por linha. As matrizes continuam com 34 IDs principais e 10 complementares. LAB-EM-CENA, ordens alternativas TVDB, identidade dividida/combinada, Usenet e atualização MPV permanecem fora. [Histórico reconciliado](auditoria/HIST_01_RECONCILIACAO_DEFINITIVA.csv)
- Recuperação: bundle cumulativo em evidencias-ml07-definitivo com checksum e log. O script recover_ml07_definitivo.py verifica SHA, recupera só em um repositório novo e usa como âncora 5cb4f2d de uma recuperação ML07 validada. Pode criar checkout novo opcional. Reusar danplus-ml05 como ambiente efetivo quando estiver limpo. Copiar arquivos de configuração privada apenas no ambiente efetivo e fora do Git. [Script de recuperação](tools/recover_ml07_definitivo.py)

Após fechar o Drive ou receber uma disposição formal do proprietário, conferir novamente a árvore e os dois certificados. Usar uma execução Gradle com os dois alvos, danplusUnifiedOta=true, arcVersionName=2.1.2, arcVersionCode=1647, console plain, máximo de 4 workers, sem daemon e com profile. Não executar clean por padrão. Conferir assinatura, package, versionCode, ABI, bibliotecas nativas, R8, SHA-256 e compatibilidade de upgrade antes de entregar os dois arquivos. Instalação e publicação ficam em autorização separada.
