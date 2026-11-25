# Checkpoint E05 — DanPlus 2.1.0

## Base e referências

- 25/11/2026. Worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, branch `feat/danplus-2.1.0-integracao-20261125`. HEAD local e remoto no precheck `37f7d0e6797ec37f10af27aba0df01ed4afaa92a`. Worktree limpa no início. Main remota `04e5b4e1780c43f6de4e5627453985821c07d48b` e local `e67459cfb1042fe2e68f97b0a017f1411512bffe`.
- Consultados AGENTS.md, checkpoints E02 a E07B-R1, matriz 146, evidências 166, MUD R3, REQ16 e BUG-THUMB-01. Lida integralmente a UI-HEADER-01 do commit documental `5ede65ac39c23a4b36f8a98458860cc6b1713506`. A imagem aprovada do cabeçalho não foi localizada no repositório, nas evidências e nos anexos consultados. Implementação guiada pela composição canônica. As notas públicas de ReSeq foram referência funcional de calendário. Pendente comparação com captura real. [PR #2](https://github.com/danilopagotto82/DanPlus/pull/2) · [ReSeq](https://github.com/abbaskip/Nuvio-ReSeq/releases)
- O TMDB documenta tags de idioma de imagem sem garantia de variante regional. O logo pt-BR vence quando identificado explicitamente pelo idioma ou pelo país BR. `pt` genérico é admissível no fallback se português for o idioma original. Original ausente ou indisponível usa título textual. A cache do Focus Dock é separada da seleção genérica. [TMDB — idiomas de imagens](https://developer.themoviedb.org/docs/image-languages)

## Entrega e rastreabilidade

| Área e IDs | Evidência e alteração |
| --- | --- |
| UI-HEADER-01 e MUD-003/004/005 | `V2TopNavigation` desenhava todo o cabeçalho em uma superfície única. Agora o logo e o relógio são independentes, Configurações saiu do menu central e ficou ao lado do avatar. A cápsula de controles tem 10% da opacidade da central, que lê o ajuste existente de 0–100%. Preservados menus reais, relógio do aparelho e demais estilos de navegação. O retorno tenta recuperar o foco quando o conteúdo ainda está sendo montado. |
| LOGO-PTBR-01, REQ16-05, MUD-008, BUG-BACKDROP-01, B3-D02 e B4-D15 | O Focus Dock usava logo de origem desconhecida e desabilitava a rede. Descobrir dependia das URLs do catálogo. Indicações usava o backdrop informado ou um cache consultado de forma síncrona. A resolução agora é iniciada pelo foco, com identidade e tipo como chaves, cancelamento ao trocar e publicação no estado observado. O mesmo caminho reaproveita `fetchPosterArt`, cache TMDB e Coil, sem ampliar prefetch. |
| B2-10, B2-16, B3-D04/05, A2-30/31/33 e RSQ-16 | Biblioteca local vazia oferece Descobrir. Corrigido o uso da ausência de exceção como confirmação de foco na coleção. Catálogos, coleções da Home e addons usam rascunho com o mesmo controle Salvar/Cancelar. Voltar cancela o rascunho. Mantidas regras de blocos por addon e o editor de pastas existente. A ordenação por catálogo verifica a seleção do perfil na gravação. |
| DP21-GRID-META-01, MUD-007B/008, REQ16-10/12/13 e B3-D06 | Grade lateral apresenta estado, ano inicial e total de episódios de série quando informados. Classificação tem selo textual e tratamento visual próprio. Sinopse, Direção e Elenco seguem no painel. Na pausa, a apresentação existente já selecionava Direção e Elenco, sem expor Writer. Ações de reprodução e de informação existentes preservadas. |
| B3-D06/07/14, B4-D16 e RSQ-19 | Cartaz da temporada via número informado pelo TMDB, inclusive especiais e fallback para o cartaz da série. Filmografia oferece Filmes/Séries e filtra apenas créditos explícitos de participação em talk shows. Trakt confirma nota pessoal de filme ou episódio identificado, separada dos agregadores e com erro explícito. Mudanças de perfil e sessão invalidam os pedidos. Preservados os modos de episódios, Em Cena permanece LAB e trailers ficam com E09. |
| RSQ-06 | Atalho Hoje usa data e fuso do contrato E06. Mantidos agrupamento diário na Lista, Semana e Mês, filtros, ordenação e entradas sem horário. O relato de séries desaparecendo não foi reproduzido estaticamente. Preparado cenário de teste com os mesmos IDs de série antes e depois de mudar filtros. |
| A2-42, REQ16-10, RSQ-01/02/03 | Descubra-me expõe filtros avançados, buscas salvas, histórico com restrição etária atual e prévia da fila de Mixes no painel existente. Operações usam o perfil, a revisão e o público atuais. Títulos assistidos usam escurecimento na imagem e indicador existente. Preservados Outra sugestão, personalização, visibilidade no menu e PIN. |
| MUD-006 e REQ16-06 | Destinatários lado a lado, logo central com fallback textual e envio em uma ação. A validação existente impede destinatário ou remetente incompatível. Estado de fila continua explícito. Nenhuma operação de implantação remota. |
| MUD-013, REQ16-04/07, BUG-THUMB-01 e A1-26 | Prévia estava presa à moldura com escala de recorte. O desenho usa encaixe integral para quadro e tira. O indicador Seekr aparece apenas no quadro remoto que contém a posição atual. Botões de configuração em linha. Verde reservado ao teste válido. Preservados gerador, prévia existente e HOLD MUD-011. |
| REQ16-02, A2-52 e contratos ASS/SSA E07B | Novas escolhas de artes oferecem Clássico e banners empacotados. Preferências e aliases antigos continuam legíveis. Editor ASS/SSA separado, com escala, posição e fonte em MPV opt-in. As fontes importadas e o fluxo de upload seguem disponíveis. Foco inicial do editor de perfis confirma a solicitação. Silo usa selo apenas para os dois identificadores reais de provedor. |
| MUD-002, A1-40 e REQ16-08 | Rascunho local do relatório preservado durante recriação de UI. Botão evita novo disparo durante envio. Respostas mantêm texto completo em lista rolável e com contraste. Banco, permissões, corpo do envio e backend preservados. |

### Pontos mantidos e limites

- B1-14/20/25, A1-28/29, A2-06/34/36/37/39 e RSQ-16 mantêm os caminhos existentes de sinopse, voz/IME, foco e arte. E04 mantém cache, controle de memória, prefetch e suspensão durante reprodução. Ganhos de fluidez, ausência de piscadas e recuperação de foco em carga fria dependem de medição em TV.
- B1-19, B1-21, A2-32, B4-D19 e o relato de séries desaparecendo seguem em investigação. Sem patch especulativo de search refresh, retorno da Biblioteca, pipeline de pôsteres, sidebar fosca ou exclusão de dados do calendário. O teclado ambiente depende de smoke. QR e upload funcionais preservados.
- A resolução estrita pode mostrar apenas o nome quando o TMDB retorna somente português sem variante para uma obra em outro idioma. O selo Seekr sinaliza uso efetivo da miniatura, separado do estado de disponibilidade mostrado na preparação. A validação de bordas e proporção ainda exige imagens 16:9 e fora desse formato.
- E02, E02-R1, E04, E03, E03-R1, E03B, E06, E06-R1, E07B e E07B-R1 permanecem na ancestralidade. Main, produção, assinatura, applicationIds, OTA, perfis, dependências e MPV 0.1.12 preservados. E07A, E08 e E09 não iniciados.

## Verificações

- **PASS estático**. `git diff --check`, revisão do índice e dos caminhos alterados, 51 arquivos Kotlin no parser PSI e quatro XML com 36 recursos novos referenciados verificados nos conjuntos padrão e PT-BR. Conferência dos dez ancestrais protegidos.
- **Testes escritos**. Nove casos novos para prioridade e fallback dos logos, participações em talk show, opacidade do cabeçalho, prévia e mudança concorrente na ordem, selo Silo, identidade de especiais e recusa de IDs ambíguos ou inválidos. Regressão de catálogo atualizada para Salvar, Cancelar e mudança de revisão de perfil.
- **FAIL**. Nenhuma falha nas verificações finais executadas.
- **NÃO TESTADO**. JUnit, resolução de tipos e DI por build, APK, dispositivo, acessibilidade física, comparação visual, rede real, memória, FPS, chamadas Trakt e Seekr. Gradle, build, instalação, push, merge e release não executados.

## Matriz de capturas E10

Capturar antes/depois no Shield Pro 2017 e Fire TV Stick 4K Max de 2ª geração, com o mesmo perfil, tema, resolução e conteúdo quando possível. Registrar versão, dispositivo, resolução, idioma, escala de fonte, overscan e cache frio/quente. Fazer sequência DPAD e Back, inclusive retorno de Detalhes, troca rápida de perfil, textos longos e foco restaurado. Comparar memória e frames durante navegação, reprodução e retorno.

| Captura | Verificação |
| --- | --- |
| Home completa e cabeçalho | Fundo claro e escuro, logo isolado, menu real em uma cápsula, sinopse, relógio 12/24 h, foco engate na engrenagem e avatar, opacidade 0/60/100, overscan e DPAD enquanto carrega. |
| Descobrir e Biblioteca | Logo explícito PT-BR, só original, português ambíguo e sem logo. Trocar foco rapidamente durante rede lenta, inclusive retorno de Detalhes. Ver biblioteca vazia e CTA. |
| Grade lateral e pausa Você está assistindo | Filme, série em andamento, encerrada, episódio e temporada especial. Conferir ano inicial, classificação, total de episódios, Direção, Elenco, legibilidade e acesso aos botões. |
| Detalhes | Cartazes com e sem dado por temporada, especiais/anime, V2 no Shield e Classic no Fire conforme configuração. Filmes/Séries na filmografia. Nota Trakt com envio, remoção, falha, troca de perfil e episódio ambíguo. |
| Calendário | Hoje em Lista, Semana e Mês. Dia sem hora, série e filme com mesma data, filtros, fuso e dias sem eventos. Conferir os mesmos IDs quando uma série deixa de aparecer. |
| Descubra-me | Filtros, histórico, buscas salvas, outra sugestão, assistidos, Mixes, perfil infantil, limite de grupo, PIN e troca A → B → A. |
| Indicações | Primeiro card sem backdrop no payload, foco rápido, selecionar e cancelar, envio uma vez e retorno. Logo, perfis lado a lado e confirmação real da fila. |
| Seekr | Chave válida, inválida e indisponível. Miniatura remota 16:9 e fora de 16:9, quadro/tira local e fallback. Moldura e indicador somente no uso remoto. |
| Configurações, DPAD e relatórios | SRT/ASS/SSA/PGS, fonte e upload, posição e opt-in MPV, arte Clássico com preferências antigas, perfis/PIN, teclado ambiente, Silo real. Reordenar 1/2/50 itens, salvar e cancelar. Rascunho, erro de envio e resposta longa. |

**Medição de backdrop.** Gravar a tela e o comando DPAD no mesmo relógio. Marcar o primeiro quadro com foco no item e o primeiro quadro onde a arte do mesmo item está visível. Em Indicações medir também desde a abertura. Registrar atraso em ms, cache, rede, recuperação, frame rate e memória, sem abrir Detalhes como parte do caminho medido.

**Próxima ação.** Auditar pelo Commander o commit filho de `37f7d0e6797ec37f10af27aba0df01ed4afaa92a`. Conferir especialmente Header, propagação de arte por foco, titularidade do Trakt e guardas de perfil e família. Aguardar a revisão do coordenador antes de publicar.
