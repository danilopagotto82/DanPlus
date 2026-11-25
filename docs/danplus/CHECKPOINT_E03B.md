# Checkpoint E03B — DanPlus 2.1.0

25/11/2026 · Candidato local · Revisão estática · Validação audiovisual no E10.

## Base e escopo

- Branch `feat/danplus-2.1.0-integracao-20261125`, worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, HEAD inicial local e remoto `7563797def5bd037b22ad932f7d468bf24eb5941`. Main remota `04e5b4e1780c43f6de4e5627453985821c07d48b`.
- E02 `af4b063`, E02-R1 `0e82f59`, E04 `b17a507`, E03 `fa2cde2` e E03-R1 `7563797` são ancestrais do candidato. Escopo confirmado com AGENTS.md, checkpoint E03 e [PR documental #2](https://github.com/danilopagotto82/DanPlus/pull/2).

## Implementação

| ID | Entrega | Arquivos centrais |
| --- | --- | --- |
| B4-D08 | Reconhece URLs HTTP(S) de BDMV, index.bdmv, PLAYLIST ou MPLS dentro da estrutura do disco. Usa escolha explícita com mais de uma candidata. Uma URL MPLS válida pode iniciar mesmo com o índice HTTP indisponível. Limita a descoberta a 64 playlists, 128 clipes por playlist, 1 MiB por metadado e prazos canceláveis. | `BlurayPlaylistResolver.kt`, `PlayerRuntimeControllerInitialization.kt`, `PlayerScreen.kt` |
| B4-D08 | Lê o IN/OUT em 45 kHz e o relógio CLPI da sequência de apresentação. Inicializa a referência temporal do TS e monta os clipes recortados em ordem na mesma timeline por Media3. Só abre clipes autorizados do disco e verifica Range `206` antes de montar a playlist. A resposta a seeks não pode ignorar o deslocamento ou redirecionar a sessão. | `BlurayPlaylistResolver.kt`, `BlurayMediaSources.kt`, `BdavM2tsDataSource.kt` |
| B4-D08 | DTS-HD leva core e extensões no mesmo sample e informa HRA/MA pelo tipo HDMV. TrueHD separa o core AC-3 dos access units. PGS forma display sets. LPCM HDMV `0x80` é ignorado na preparação e gera aviso na playlist. Leitores BDAV entram por detecção de pacotes no clipe direto e preservam os decoradores de vídeo. TS comum mantém o extrator original. | `BdavDtsHdReader.kt`, `BdavTrueHdReader.kt`, `BdavPgsReader.kt`, `BlurayMediaSources.kt`, `PlayerMediaSourceFactory.kt`, `DolbyVisionExtractorsFactory.kt` |
| B4-D09 | Guarda posição, play/pause, motor, velocidade, atraso de legenda e seleção de trilhas antes de abrir o extra. Fim, erro ou Voltar usam a mesma retomada única. O ticket da sessão, a geração da inicialização e a instância do player descartam eventos tardios. Guardas suspendem progresso, assistido, scrobble, PostPlay e persistência de trilhas do longa durante o extra. O ponto do longa é salvo antes da transição. | `PlayerRuntimeControllerDiscTitles.kt`, ciclo de vida/eventos/recuperação do player e `PostPlayRecommendationController.kt` |

Upstream consultado para B4-D08: `4bd2502d2f87`, `f34cb4a5660d`, `e4a1bdc49a8b`, `f53cb6195aff` e `927e81575ae0`. Para B4-D09: `ea3193907b75`, `0ae08ec0dbed` e `98e05d4ea321`. Os leitores derivados e suas regressões têm atribuição GPL-3.0 no código. [Seek/CLPI upstream](https://github.com/NuvioMedia/NuvioTV/commit/f53cb6195aff) · [Leitores de áudio e LPCM](https://github.com/NuvioMedia/NuvioTV/commit/927e81575ae0) · [Retorno após término](https://github.com/NuvioMedia/NuvioTV/commit/98e05d4ea321)

## Limites e riscos

- Playlists usam ExoPlayer na sessão, com MPV `0.1.12` e a preferência de motor persistida preservados. O extra só abre fora de uma sessão em grupo. O fluxo de faixas e legendas embutidas permanece ligado ao player. Legenda externa que exige recarregar a fonte convencional é recusada na playlist de disco.
- A descoberta espera um índice HTTP em PLAYLIST com links relativos reconhecíveis. Uma URL MPLS explícita contorna a ausência do índice. Redirecionamentos e servidores sem Range são recusados no caminho de playlist. CLPI ausente, relógio multissequência e multiângulo exigem implementação adicional. A busca usa o seek TS do Media3 com Range; o mapa de pontos de entrada do CLPI foi avaliado como otimização futura.
- A aceitação técnica em runtime depende do alinhamento CLPI/PCR no arquivo real, do comportamento do servidor em seeks não zero, da seleção entre períodos e dos decodificadores disponíveis. LPCM nesse demux é indisponível. Uma faixa alternativa, quando houver, deve ser usada. Nenhuma inferência de passthrough por aparelho.
- Contrato com o futuro E06. A entidade registrada continua sendo o longa. Durante `discExtraId` ativo, eventos do extra ficam fora de histórico, progresso, assistido, scrobble, Continuar Assistindo e PostPlay. Mudanças posteriores nesses produtores precisam manter a guarda e a restauração da posição do longa.

## Verificação e regressões

- **PASS estático** com revisão integral do diff, `git diff --check`, análise sintática de 30 arquivos Kotlin sem erros, paridade das 14 chaves XML em inglês e português e checagem das assinaturas do extrator local.
- **FAIL** nenhum nas verificações executadas.
- **NÃO TESTADO** JUnit, Gradle/build, APK, instalação e reprodução no dispositivo. Foram preparados 26 casos em cinco arquivos. Cobrem playlist válida, ambígua, ausente, truncada e malformada, lista de clipes e recortes, recusa de redirects e servidor sem Range, DTS-HD/TrueHD/PGS, tratamento de LPCM, retorno único e callback tardio.

| E10 | Cenário preparado e aceite |
| --- | --- |
| Timeline e fronteiras | Servir playlist de dois clipes com IN não zero, OUT antes do fim e um clipe repetido. Verificar primeiro e último frame do intervalo e seeks a `fronteira − 1 s`, na fronteira e `fronteira + 1 s`, nos dois sentidos. O relógio, o áudio e a legenda seguem o clipe ativo. Repetir com HTTP 200 em Range e verificar erro controlado. |
| Trilhas | Em arquivo com DTS-HD MA/HRA, TrueHD e PGS, conferir enumeração, mudança de faixa e apresentação no aparelho. Com LPCM e trilha alternativa, a abertura conclui e a alternativa toca. Em LPCM-only, informar a limitação e preservar o controle do player. |
| Extras | Repetir fim, erro, Voltar, duplo evento, callback tardio e perfil/saída concorrente. Começar o longa tocando e pausado. Confirmar posição restaurada, trilhas quando disponíveis e nenhum registro do extra no histórico, assistido, scrobble, PostPlay ou Continuar Assistindo. |
| Controles E02/E04/E03 | MKV/REMUX, TS comum, M2TS direto, HLS/DASH e MPV convencional. Conferir escolha Dolby/DTS, DV7/HDR, AFR inteiro/fracionário/restauração, seek/cache/memória e falha de fonte com janela de 120 s. |

Sem Gradle, build, instalação, push, merge ou release. Sem alteração de main, dependências, perfis, assinatura, OTA, aplicação em produção ou E06. ISO/Usenet segue em LAB.

Próxima ação: coordenador auditar o commit local E03B contra `7563797` via Commander. Após a revisão, decidir a liberação do push e encaminhar a matriz de reprodução ao E10.
