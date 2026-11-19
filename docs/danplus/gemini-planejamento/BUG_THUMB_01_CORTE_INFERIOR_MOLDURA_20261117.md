# DANPLUS P−1 — BUG-THUMB-01 · Recorte inferior da miniatura (avaliação estática)
Data 17/11/2026. Origem: screenshot enviado pelo proprietário, relato: "quadro da miniatura corta parte de baixo; subir não parece resolver".

## Identificação e separação
- Defeito **visual de apresentação** da miniatura, NÃO defeito comprovado do gerador, download, Seekr, cache ou motor de miniaturas.
- Associar à decisão RS-04 **MANTER** o sistema de miniaturas do DanPlus. Isto NÃO significa manter o corte visual; o bug tem ID independente `BUG-THUMB-01` sem alterar os 7 RS nem os 146 P−1.
- Propriedade e intensidade exatas do recorte precisam de comparação da tela real e imagem original; análise do código identifica hipóteses, NÃO causa confirmada.

## Trechos concretos da base protegida
- `app/src/main/java/com/nuvio/tv/ui/screens/player/SeekThumbnailPane.kt`:
  - `SeekThumbnailOverlayHost`: imagem com `ContentScale.Crop` dentro de `Box(height = 108.dp ou 127.dp, width = height * aspect)`, com `.thumbnailSurface(shape)`; se proporção calculada não corresponder à imagem, os extremos podem ser cortados.
  - Para preview Seekr remoto: `aspect = 16f / 9f` obrigatório mesmo que o bitmap tenha outra proporção; hipótese importante para recorte **interno** da imagem.
  - Local `croppedPainter(shown)`: aplica `shown.crop` calculado para tirar barras pretas, antes do `ContentScale.Crop`.
  - `AboveSeekBar`: ancora janela nas coordenadas `SeekBarAnchor.bounds`, aplica padding inferior BarGap + ThumbnailFrameClearance e `.clipToBounds()` na área; pode cortar a **moldura** se suas coordenadas/bounds divergirem.
  - Modo tira tem outro layout `SeekThumbnailStrip` com tamanho de centro 127.dp e tiles 95.dp, todos em `ContentScale.Crop`. O bug pode ser exclusivo do quadro central ou também da tira.
- `app/src/main/java/com/nuvio/tv/core/player/thumbnail/ThumbCrop.kt`: `ThumbCrop.Estimator` identifica bordas escuras e remove parte delas em **tempo de exibição**, não apaga o bitmap salvo; se uma cena escura parecer borda, investigar falso-positivo.
- `PlayerScreen.kt`: `ProgressBar` registra `SeekBarAnchor.bounds = it.boundsInRoot()` e `SeekThumbnailOverlayHost` é desenhado na tela do player; avaliar transformação de coordenadas e se o player foi reduzido/tem padding, sem mexer cegamente na posição.
- Base: `D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115`, branch `danplus/2.0-ml01-integracao`, HEAD `0ce90a0740656c903b9780d2d423966b15119697`. Só existe `?? .wrangler/` pré-existente no Git.

## Rota mínima para corrigir FUTURAMENTE — NÃO EXECUTAR NESTA ETAPA
1. Diferenciar visualmente: **a imagem dentro da moldura está cortada?** ou **a borda inferior da moldura some?** Capturar screenshot real com foco do player na mesma posição, render local/Seekr e compará-lo com o thumbnail original.
2. Testar dimensões de origem do bitmap (largura/altura e `crop`), `aspect` escolhido, bounding box da moldura, coordenadas barra/moldura e escala. Repetir vídeo widescreen e 16:9, modo quadro único e modo tira, Shield e Fire.
3. Se for recorte do **conteúdo**: avaliar `ContentScale.Fit` e proporção real do frame, limitar largura/altura pela tela sem zoom forçado; validar bordas pretas e sistema atual de detecção de barras. Preservar visual premium, cantos arredondados e bordas.
4. Se for corte da **moldura**: corrigir ancoragem/bounds/clip do container, área de segurança inferior e z-index, sem apenas subir o quadro com offset fixo; evitar cortar progress bar e outros HUDs.
5. Se for ambos, aplicar correções mínimas independentes e comparar screenshots antes/depois com o design aprovado. Não desligar globalmente o gerador, Seekr ou os limites de memória.

## Critério de aceite futuro
- Uma imagem 16:9 e outra com proporção diferente devem ficar totalmente enquadradas dentro da moldura (salvo crop deliberado de barras pretas verificado).
- Bordas inferiores completas e visíveis, espaçamento consistente sobre barra de progresso, posição correta perto do início/fim da linha, sem sobreposição de texto ou perda de foco.
- Mesma fidelidade em Shield Pro e Fire TV 4K Max, tanto na Home clássica se aplicável quanto nos estilos DanPlus; 1080p e 4K; sem perda de fluidez.
- Evitar alterações em captação de miniaturas, Seekr, player, cache, desempenho, fonte e áudio fora do escopo.

## Estado
- Análise estática de trechos: **PASS**.
- Localização exata da causa: **HIPÓTESES, NÃO COMPROVADA**.
- Teste de screenshot comparativo, build, testes, smoke e aparelhos: **NÃO EXECUTADOS**.
- Código, branch, Git, APK, OTA, perfis, publicação: **PRESERVADOS**. Não há autorização para implementar.
