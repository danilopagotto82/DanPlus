# Checkpoint E03 — DanPlus 2.1.0

25/11/2026 · Candidato revisado estaticamente · Homologação reservada ao E10.

## Base

- Worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, branch `feat/danplus-2.1.0-integracao-20261125`.
- HEAD inicial local e remoto `b17a507ca3d028a73064fd081a8ca17e9cedd5d4` (E04). Worktree limpa no precheck. E02 `af4b063`, E02-R1 `0e82f59` e E04 são ancestrais do commit que contém este checkpoint.
- AGENTS.md, checkpoints E02/E04, plano, matriz 146, evidências 166 e [plano complementar](https://github.com/danilopagotto82/DanPlus/pull/2) consultados na revisão documental `5ede65ac39c23a4b36f8a98458860cc6b1713506`.

## Correções e rastreabilidade

| IDs | Mudança e alcance |
| --- | --- |
| B1-07, B1-13, A1-11 | O sufixo literal do manifesto na URL tem prioridade sobre um nome de arquivo conflitante do addon. URLs opacas continuam usando as inferências existentes. A correção evita escolher o parser HLS para MPD, ou o inverso, nesse conflito específico. |
| B1-12, A1-04, A1-11, A1-18, A2-24 | O fallback de inicialização MPV → ExoPlayer reaproveita o seek pendente, a posição disponível ou a posição carregada pela recuperação antes de liberar o motor. A janela E02-R1 continua com o mesmo prazo global de 120 s. Watchdog e sincronização existentes foram revisados. Correção de posição entregue, travamento e sincronia física pendentes. |
| A1-03, A1-15, A1-16, A2-17, A2-22 | A preferência por modo sem interrupção ou pelo dobro da frequência respeita a cadência quando há um modo mais preciso. Retorno ao modo original, modos anunciados pelo display e preferência AFR permanecem nos caminhos existentes. Saltos periódicos e áudio durante troca dependem da cadeia HDMI. |
| A2-01, B4-D14 | A seleção lossless considera o subtipo DTS detectado nos frames. DTS-HD HRA e DTS Express saem da categoria lossless. MA e DTS:X detectados preservam prioridade. Ordem das escolhas explícitas, idiomas, contratos de apresentação e roteamento de áudio preservados. Badges e tecla física seguem no escopo próprio. |
| A2-09, B4-D08 | Clipes HTTP(S) indicados como `.m2ts` ou `.mts`, com MIME TS, são reconhecidos pela cadência dos bytes de sincronismo. O wrapper entrega pacotes de 188 bytes ao extrator e traduz offsets e comprimentos de seek. TS comum mantém seus bytes. Cache, contagem de transferência e transportes continuam em coordenadas brutas. [Extrator TS Media3 1.8.0](https://github.com/androidx/media/blob/1.8.0/libraries/extractor/src/main/java/androidx/media3/extractor/ts/TsExtractor.java), [referência upstream para BDAV](https://github.com/NuvioMedia/NuvioTV/commit/4bd2502d2f87). |

## Pontos revisados com validação pendente

| IDs | Evidência e pendência |
| --- | --- |
| B1-02, A1-19, A2-03, A2-07, A2-12, A2-20, A2-51 | Política de passagem Dolby/DTS, promoção de trilha, áudio após retomada e acompanhamento da saída MPV já têm caminhos próprios. O patch atua na classificação DTS para seleção. Dolby Digital/DD+, DTS, AAC 5.1, PCM, TrueHD/Atmos, ARC e retomada dependem de fixture, aparelho e caminho de saída. |
| B1-23, A1-11, A1-24, B3-D01 | Caminhos de AV1/HDR10+ e configuração de conversão/base layer DV7 revisados. A comprovação exige fixture e sinal HDMI. Tratamento HEVC existente e orçamento E04 preservados, sem adicionar JNI ou reflexão. |
| A1-17, A2-10, A2-18 | Abertura MKV/Plex, heurística VC-1, fallback por formato e seek existentes. Traçar no E10 tempo de abertura, formato/decodificador efetivo, continuidade de posição e efeito no cache. |
| A2-05, A2-19 | Preferências por perfil e restauração de trilha permanecem no fluxo existente. Testar após troca de motor, pausa e reinício. |
| A2-08, A2-11 | Investigação somente. PCM multicanal, bitstream e decodificação direta exigem decisão do coordenador antes de qualquer experimento. |
| B1-05, B1-17, B4-D08, B4-D09 | Preservados pós-créditos e capítulos existentes. Foram avaliados o parser BDMV/MPLS, o leitor DTS-HD e a sessão de disco upstream (`4bd2502d2f87`, `927e81575ae0`, `ea3193907b75`, `0ae08ec0dbed`, `98e05d4ea321`). A adaptação deste lote cobre o transporte de clipes. A integração de playlist/extras exige sessão de disco e isolamento do histórico e scrobble. DTS-HD, TrueHD, PGS e LPCM no disco dependem dos leitores e do fixture. A limitação upstream de LPCM e ISO/Usenet ficam fora do patch. |
| B4-D14 | Avaliados `4db6cbabdf8e`, `8efa5ca57c33`, `8b6ac91b16aa` e `928244de2d75`. O consumidor de classificação usa o subtipo técnico já presente. Só a classificação DTS mudou. |

## Avaliação MPV — B2-18

- DanPlus usa `io.github.abdallahmehiz:mpv-android-lib:0.1.12`. O AAR local inclui `libmpv.so` e `libplayer.so` em `armeabi-v7a`, `arm64-v8a`, `x86` e `x86_64`. Comparado com `0.2.1`, versão declarada na [beta.4 Nuvio](https://github.com/NuvioMedia/NuvioTV/blob/1.2.0-beta.4/app/build.gradle.kts).
- O candidato migra a biblioteca para Kotlin Multiplatform com variante Android, altera fluxos de propriedades e traz mudanças upstream relacionadas a HTTP/2, Vulkan e Rubber Band. Há potencial em rede e processamento. Compatibilidade com os callbacks, uso de CPU/memória, desempenho e áudio DanPlus exigem comparação. O fluxo upstream prevê as quatro ABIs, inclusive ARM de 32 bits por padrão. Conteúdo ELF, símbolos JNI, dependências nativas e alinhamento do AAR candidato ainda precisam de inspeção. [Comparação upstream](https://github.com/abdallahmehiz/mpv-android/compare/v0.1.12...v0.2.1), [workflow de publicação](https://github.com/abdallahmehiz/mpv-android/blob/v0.2.1/.github/workflows/build_release.yml).
- POMs declaram MIT para o artefato. O upstream identifica condições GPL para FFmpeg/libmpv na configuração distribuída. O gate de atualização precisa conferir avisos, configuração e fontes correspondentes do artefato exato. Manter `0.1.12` neste lote e avaliar `0.2.1` em branch isolada, com ABI, ciclo de superfície e trilhas, inicialização, retorno do background e fallback validados. [Licenças upstream](https://github.com/abdallahmehiz/mpv-android/blob/v0.2.1/docs/licenses.html).

## Regressões e gates

Cinco arquivos de teste preparados, dez casos novos. Cobrem manifestos conflitantes, candidatos BDAV HTTP, bytes e seek de BDAV versus TS, DTS HRA/Express e DTS:X, prioridade de posição no fallback e cadências inteiras e fracionárias.

| E10 | Verificação a executar |
| --- | --- |
| Áudio | Alternar AC-3, DD+, DTS, DTS-HD HRA/MA, TrueHD/Atmos, PCM e AAC 5.1 em MKV e clipes compatíveis. Iterar pausa/retomada e AFR com áudio ativo. Conferir decodificação, canais efetivos no receiver/soundbar e idioma/trilha escolhidos. |
| MPV e E02/R1 | Abrir formato com resgate MPV, provocar falha ainda na abertura com posição conhecida e confirmar MPV → ExoPlayer no mesmo título, incluindo seek a zero. Observar watchdog, sincronia, interface, limite total de 120 s e ausência de retorno em laço. |
| Vídeo, HDR e E04 | Abrir HLS/DASH assinados com MIME conflitante, MKV/Plex, VC-1 e 1080i. Reproduzir AV1 SDR, AV1 HDR10+ e DV7 nativo/convertido com fixture identificado. Registrar primeiro frame, seek, rebuffer, HDR anunciado pelo display e PSS. |
| AFR e preferências | Rodar 23,976/24/25/29,97/30/50/59,94/60 conforme modos anunciados. Observar cadência, áudio, restauração do modo original ao sair e persistência do perfil, mantendo QMS condicionado à cadeia HDMI. |
| Blu-ray | Servir clipe BDAV e TS comum via HTTP com Range. Conferir seek inicial, médio e final, tentativa sem Range e clipe maior. Inspecionar DTS-HD/TrueHD/PGS/LPCM conforme fixture. BDMV/MPLS/extras requerem gate próprio, incluindo volta ao longa após término/erro e histórico sem progresso indevido dos extras. |

Equipamentos E10 de referência: Shield Pro 2017, TCL 65C715 e Denon S510BT com REMUX. Fire TV Stick 4K Max 2ª geração, Samsung UN55MU6300 e HW-Q600F.

- **PASS estático** após revisão do diff, `git diff --check` e análise sintática Kotlin.
- **FAIL** nenhum nas verificações executadas.
- **NÃO TESTADO** JUnit, Gradle/build, APK, instalação, smoke e reprodução física. Gates de hardware, codec, dados e desempenho seguem no E10.
- E02, E02-R1 e E04 preservados. Sem push, merge, release, alteração de main, versão, OTA, assinatura, IDs, presets ou produção. E06 e descarte remoto Silo fora da execução.
- Próxima ação: auditoria independente do commit local pelo coordenador contra `b17a507`. Manter push em espera até a decisão do coordenador e reservar a matriz audiovisual para o E10.
