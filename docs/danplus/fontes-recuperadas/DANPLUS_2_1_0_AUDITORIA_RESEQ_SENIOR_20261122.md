# DANPLUS 2.1.0 — AUDITORIA SÊNIOR DO FORK NUVIO-RESEQ · INTEGRAÇÃO E EXPERIÊNCIA PREMIUM

**Data:** 22/11/2026  
**Escopo:** auditoria funcional, arquitetura e UI do fork completo `abbaskip/Nuvio-ReSeq`, incluindo histórico de releases, código, README, TV_MIX, TIERED_AUTOPLAY, UPSTREAM_SYNC, BUILD_PIPELINE e calendário.  
**ReSeq analisado:** tag pública **v1.0.0-rc.3.1** (publicada em 22/11/2026), commit árvore `ba3a28278328c6eef937a78dd3b326c5b54dc39c`.  
**DanPlus analisado:** GitHub `danilopagotto82/DanPlus`, `main`, árvore `3715ee0de8345d617f48c2da7271716ed501dc5c`.  
**Autorização nesta etapa:** o proprietário pediu **incluir todas as ideias na avaliação e definir como aproveitá-las**, preservando identidade, funcionalidades homologadas e fronteiras entre projetos. **Não autorizou merge, compilação, instalação, alteração de GitHub ou publicação.**

## 0. VEREDITO EXECUTIVO

**Estratégia:** `ADAPTAR DELTAS`, não `MERGE DO FORK`. O DanPlus já possui Scout, Surpreenda-me, filtros, misturas, histórico/Trakt/Simkl, Calendário avançado, imagens cinematográficas, relatórios, backups e parte do processamento de mídia. Importar telas e módulos inteiros do ReSeq duplicaria serviços, modelos, caches, estados de perfil e caminhos de player.

**Melhor evolução de produto:** Scout = **Descubra-me** como identidade de produto; usar o motor atual `ScoutEngine`, `ScoutPicksFinder`, `ScoutViewModel`, `ScoutMixStore` e fluxo existente de fontes. Acrescentar capacidade FlickPick apenas onde não existe: histórico curto de sugestões, um sorteio que respeita filtros e já assistidos, seleção por listas, cartão com **Assistir / Outra sugestão / Adicionar**, modos filme e episódio e integração com descoberta avançada. `ScoutScreen.kt` já tem `surprise`; nunca criar um segundo motor de sorteio.

**Visual:** preto quase puro, dourado discreto, tipografia nítida, fundo com arte cinematográfica bem contrastada, sinopse visível e animações DPAD moderadas. **Não copiar** a tela vinho/serifa/cartões fixos do FlickPick; reutilizar componentes DanPlus e manter experiência responsiva na Fire.

**Calendário:** DanPlus já apresenta Lista/Semana/Mês, mosaico de artes, backdrop de destaque, lembretes, fontes Trakt/Simkl/TVmaze/TVDB e cinema/digital/Blu-ray. ReSeq tem Dia/Semana/Mês e agenda unificada de episódios, cinema, digital, disco e esportes, com cartões compactos. **Preservar apresentação DanPlus; adotar do ReSeq somente interações e organização que constituam delta.** Esportes/EPG pertencem ao DAN TV+ e não justificam importar subsistema Live TV para DanPlus. **Não há screenshots atuais equivalentes dos dois APKs: comparação visual é análise de código, não avaliação pixel a pixel.**

**Status geral:** Auditoria de leitura de código e documentação — **realizada**. Correspondência com a 2.0.2 instalada, desempenho, testes Gradle, smoke Fire/Shield e aparência real: **NÃO TESTADOS**.

## 1. MATRIZ COMPLETA RESEQ — PROPOSTAS APROVADAS PARA AVALIAÇÃO

A numeração `RSQ-xx` é **catálogo complementar**, fora dos **146 IDs já classificados**. Cada item tem uma única implementação responsável; `AVALIAR` não significa importar automaticamente.

| ID | Ideia ReSeq | Estado observado no DanPlus | Veredito de produto e implementação | Dono técnico único / gate |
|---|---|---|---|---|
| RSQ-01 | FlickPick — descobrir filme/episódio e reproduzir | **Scout/Surpreenda-me já existem** | **ADAPTAR**: memória de 20 sugestões por perfil, filtros, listas e botão Outra; usar motor Scout e seletor existentes; sem novo player | E07B-Scout (Codex), E05 só apresenta componentes visuais, teste perfil/DPAD |
| RSQ-02 | Advanced Discover (termos exatos, TMDB keywords, AND/OR, filtros combináveis, salvar busca) | Descobrir/Scout e filtros existentes; palavras-chave avançadas não comprovadas | **ADAPTAR**: modo simples e avançado sobre o mesmo catálogo/serviço, consultas limitadas e busca salva por perfil | E07B-Scout/Discover, sem segundo backend |
| RSQ-03 | TV Mix Play/Remix/preview e sequência aleatória | DanPlus já tem `ScoutMix`, `ScoutMixStore`, `ScoutMixSession`, misturas prontas | **ADAPTAR**: mostrar próxima seleção, Remixar, salvar, reproduzir/continuar via caminhos existentes | E07B-Scout; reproduções/regressão com E02/E06, sem duplicar mix |
| RSQ-04 | Continue Watching com episódios fracionados, retomada e TVDB verificada | Histórico e próxima reprodução já existem; não há prova de equivalência completa | **INVESTIGAR → ADAPTAR DELTA**: preservar identidade canônica; resolver conflitos de episódios sem reescrever histórico | E06, testes negativos de identidade e Trakt/Simkl |
| RSQ-05 | Adiar atualizações pesadas da Home/launcher durante playback | DanPlus já tem sincronizações e observadores, eficácia não medida | **ADAPTAR somente se medido**: atualizar progresso essencial durante vídeo, postergar trabalho volumoso até sair; RAM Fire/REMUX intocados | E04 (agendamento/perf) com contrato E06 (persistência) |
| RSQ-06 | Calendário Dia/Semana/Mês, filtros rápidos, cinema/digital/disco, eventos familiares/esportes | DanPlus já tem Lista/Semana/Mês e cinema/digital/Blu-ray, mosaicos, hero | **ADAPTAR INTERAÇÃO**: manter estética, somar botão Hoje, filtro contextual e prévia melhor; esportes só no DAN TV+ | E06 dados, E05 apresentação visual (sem sobrepor arquivos) |
| RSQ-07 | Avaliar no Trakt/Simkl/MDBList e prompt opcional pós-filme | Sistema Scout tem gosto/avaliações; integração direta não homologada | **INVESTIGAR e ADAPTAR**: uma ação de avaliar, serviços independentes, consentimento e estados por serviço; prompt DESLIGADO inicialmente | E06 tracking, E05 componente apenas após contrato pronto |
| RSQ-08 | Relatórios técnicos mais completos do player e erro | DanPlus já tem `IssueReports.kt`, requisitos de relatório e stats | **ADAPTAR DELTA**: registro de formato/decoder/buffer/taxa de quadros, anonimização, exportação revisável sem QR obrigatório | E08, proibido vazar URL/credenciais |
| RSQ-09 | Automação/ledger upstream, proteção PR, builds de QA | Monitor Original+ARC já planejado em E11 | **ADAPTAR**: monitor triplo Original+ARC+ReSeq; uma tabela de SHAs/feature/decisão/patch-equivalent e PR de revisão; sem integração cega | E11, workflow desligado até gate |
| RSQ-10 | TheTVDB ordenações alternativas e mapeamento de episódios | DanPlus tem metadados TVDB/calendário, não confirmado selecionador de ordenações do ReSeq | **LAB SEPARADO**: primeiro modelo identidade/numeração e compatibilidade addons; feature opcional por série, sem reescrever rastreamento | E06-LAB; API/key/licença e testes antes de UI |
| RSQ-11 | Autoplay em camadas | DanPlus já tem regras de fonte e REMUX | **ADAPTAR SOMENTE CAMADAS SEGURAS**: prioridade do perfil, fonte autorizada e fallback dentro dos limites; **NÃO portar** modo que ignora filtros/qualidade | E02, com teste REMUX/Debrid/Fire |
| RSQ-12 | Usenet NNTP/NZB/RAR/7z com seek | Não comprovado no DanPlus; alta complexidade | **LAB ISOLADO — BAIXA PRIORIDADE**: analisar demanda, licenças/dependências, memória e segurança. Não entrar na 2.1.0 principal sem evidência | E02-LAB opcional, sem tocar player homologado |
| RSQ-13 | CloudStream: filtros idioma, DNS, configurações de plugins e diagnósticos | DanPlus já tem ponte `.cs3`; equivalência desconhecida | **INVESTIGAR + ADAPTAR DELTA**: idioma PT-BR dentro do provider, configuração por plugin e logs sanitizados; DNS como opt-in informativo | E02 plugins, proteção contra credenciais em logs |
| RSQ-14 | Backup/sync Google Drive com conflitos e preferências por perfil | DanPlus possui backup próprio/Supabase | **EXTRAIR PADRÕES, NÃO OUTRO BACKUP**: conflito, exportação portátil opcional, proteção das chaves, compatibilidade/rollback; não ativar Drive paralelo | E08, RLS/criptografia/gate de produção |
| RSQ-15 | Live TV, EPG, Sports Hub, Multiview, M3U/Stalker/Xtream, gravações | Projeto independente **DAN TV+** já existe | **ENCAMINHAR AO DAN TV+** após auditoria de seu código/roteiro; não incluir motor IPTV no DanPlus | DAN TV+ (missão/documento distinto; sem editar por este plano) |
| RSQ-16 | Assistência a navegação DPAD, foco, revisão visual, cache e posters | E05/146 IDs já abrangem foco, backdrop, visual | **CONSOLIDAR E NÃO DUPLICAR**: usar como casos de teste e referências para E05 | E05, screenshots reais Fire+Shield |
| RSQ-17 | OTA: artefatos por ABI, atualização retomável e verificação | E10/E11 e OTA 2.0.2 já homologada; riscos de assinatura | **ADAPTAR GARANTIAS**: checksum, signer, versionCode, pacote, migração segura, release opcional | E10/E11, sem auto-publicação |
| RSQ-18 | Performance guia/EPG, armazenagem, grandes playlists e multiview | Não é a mesma área do DanPlus; DAN TV+ tem plano próprio | **ENCAMINHAR AO DAN TV+**; avaliar técnicas genéricas de banco e jobs sem carregar IPTV na 2.1.0 | DAN TV+ posteriormente |
| RSQ-19 | Detalhes melhorados: trailer contínuo, episódio, poster de temporada, legendas | Nuvio Original beta.3 já auditado e parcialmente presente | **DEDUPLICAR** com catálogo Original/ARC e MUD; adaptar só comportamento ausente; não copiar implementação dupla | E05 visual / E09 trailers / E07B legendas conforme dono |
| RSQ-20 | Correções pequenas de players, addons, legendas, torrent e histórico dos betas | Muitas entre 146/ARC.3/Original beta.3 | **DEDUPLICAR** por comportamento/teste/commit, preservar o melhor estado final; sem novos IDs técnicos só por changelog | E00 decide, E02/E03/E04/E06 executam donos exclusivos |

**Regra de tradução:** só strings ligadas a funcionalidades de fato adotadas e PT-BR; traduções estrangeiras negadas `B1-29/30/31, B2-19/20` permanecem recusadas. A marca ARC/Nuvio na interface de produto deve virar DanPlus quando pertinente, mas licenças/créditos/nomes próprios técnicos permanecem.

## 2. CALENDÁRIO — DIREÇÃO VISUAL APROVADA PARA PROTOTIPAGEM, NÃO PARA CODIFICAR

**Comparação observável pelo código:**

- DanPlus `CalendarScreen.kt`, `CalendarMonthView.kt`, `CalendarWeekView.kt`, `CalendarArtwork.kt`: painel com backdrop sincronizado ao foco, mosaic de artes nos dias, hero com logo/episódio, lista lateral, modo Glass/V2 e ajustes de dispositivos modestos (`performance`). Datas armazenadas incluem `CalendarEntryKind.MOVIE_THEATRICAL`, `MOVIE_DIGITAL`, `MOVIE_BR_RELEASE`. A lógica já diferencia episódios e filmes, horas confirmadas/estimadas, lembretes e dias sem data.
- ReSeq `livetv/calendar/CalendarScreen.kt`, `CalendarViews.kt`, `CalendarDetailPreview.kt`: filtros Tudo/Séries/Filmes/Esportes, Dia/Semana/Mês, cartões compactos com imagem, data e categoria, data escolhida preservada e detalhes no topo. Modo Dia usa quatro cartões por linha; modo Mês usa 7 colunas de dias. Mais denso; não é automaticamente mais bonito na TV da sala.
- Sem screenshot **atual** do APK 2.0.2 em Fire/Shield nem screenshot real do RC3.1 na mesma tela. Não declarar aprovação visual/PASS sem ADB e comparação pareada. Screenshots antigos `docs/screenshots/nt5/` não equivalem a 2.0.2.

**Novo conceito de UI:**

1. Cabeçalho `Calendário` com mês, controle **Hoje**, seletor `Agenda · Semana · Mês`; **Agenda** mantém comportamento atual de lista (não inventar segundo fluxo Dia para fazer o mesmo).
2. Filtros discretos `Tudo · Séries · Filmes`; filmes incluem `Cinema · Digital · Blu-ray` por subfiltro opcional. Esporte **não** aparece no DanPlus sem uma integração de produto independente autorizada; preferir DAN TV+.
3. Coluna/mosaico de datas organizada com tipografia limpa e pôsteres de proporção consistente. Dia focado ganha borda dourada sutil e foco DPAD estável; dias sem dados ficam legíveis, sem artes falsas.
4. Hero lateral/detalhes mostra backdrop correto e logo PT-BR quando possível (respeitando regras específicas de imagem), título, SxxExx, estado de assistido e disponibilidade confirmada. **Sinopse permanece visível**, sem foto tapando legenda ou card.
5. Diferenciar **data oficial**, **horário estimado** e **sem data**. Não confundir data de cinema brasileira com estreia mundial nem data digital com disponibilidade efetiva no player.
6. Fire/baixo RAM: reduzir arte simultaneamente decodificada e trabalho de mosaico mantendo composição visual; nenhuma imagem de outro perfil/título após troca rápida DPAD.
7. Legibilidade 1080p/overscan, passagem horizontal/vertical com controle, retorno do foco, pt-BR, navegação sem scroll involuntário; teste em listas longas e vazias.

**Aceite E05/E06:** screenshot real antes/depois em Shield e Fire, igualdade de eventos/datas, tempo até navegação útil igual ou melhor que baseline medido, sem flashes e sem corte, caches por título/perfil seguros, sem regressão do calendário atual.

## 3. DESCUBRA-ME — UX E FLUXO TÉCNICO

**Entrada única:** menu `Descubra-me` (avaliar renomear apenas a **marca visível** Scout/Para você; manter classes internas enquanto não houver motivo). Tela inaugural oferece ações grandes: `Surpreenda-me`, `Um episódio`, `Para todos`, `Cabe na minha noite` e acesso discreto `Refinar escolha`. O Scout existente já tem escolhas equivalentes; inspecionar cada antes de criar novo chip.

**Navegação:** selecionar modo → usar histórico/restrições por perfil → sugerir um título com backdrop e sinopse → `Assistir` (abrir escolha normal de fonte), `Outra sugestão` (deduplicar até 20 recentes, TTL), `Adicionar` (usar lista atual), `Por quê?` (explicar preferência). A opção `Buscar por critérios` usa Descobrir avançado sem ter motor novo. Para série, escolher episódio respeitando ordem e progresso já homologados; nada de episódio aleatório no lugar do próximo episódio sem opção explícita. Perfis infantis e PIN são gates duros, não filtros opcionais.

**Visual:** arte grande e escura, painel limpo à esquerda, CTA dourado discreto, títulos curtos, sinopse sempre presente, no máximo 2 ações primárias visíveis; chips só onde necessários. Não copiar layout de duas colunas carregadas do ReSeq; reutilizar `ScoutScreen` e linguagem de cartas/Glass que já existe. Cache/preload respeita memória da Fire e trocas rápidas de foco.

**Não criar:** segunda base de filmes assistidos, segundo banco de misturas, segundo gerenciador de TMDB, segundo seletor de fontes, bibliotecas de perfil duplicadas, duplicação `ScoutMixSession`, duplicação de comandos `Remix`.

## 4. FRONTEIRAS E SEQUÊNCIA DE IMPLEMENTAÇÃO, APÓS GATE E00

| Ordem | Entrega combinada | Executor único de patch | Dependência e prova antes de passar |
|---|---|---|---|
| 0 | E00 atual: mapear os 20 RSQ contra 146 + REQ16/MUD; verificar 2.0.2 e fonte sanitizada, hotspots de Kotlin e screenshots disponíveis | ChatGPT normal GPT-6 Alto + Commander **somente leitura** | Matriz `EXISTE/PARCIAL/AUSENTE/NÃO APLICÁVEL/LAB` por arquivo, SHA, riscos, nenhuma alteração |
| 1 | E01–E04: estabilidade, fonte, áudio, RAM, trabalho de fundo/segurança | Codex Alto, um sublote de cada vez | Teste sintético + físico daquilo que muda; REMUX bitstream preservado |
| 2 | E06-A: episódios/canonicalização/Continua assistindo, avaliações e dados de calendário | Codex Alto | Contratos de dados e regressões; sem editar telas do E05 |
| 3 | E05: Calendário visual, backdrops/Focus Dock/Grade e componentes visuais Descubra-me | Codex Alto | Screenshots reais, DPAD, legibilidade, composição e cache; não recriar motor |
| 4 | E07B-SCOUT: filtros, seleção, mistura/Remix e continuidade de Descubra-me | Codex Alto | Reuso explícito Scout; travas de perfil, listas, filmes já vistos, Next Up, fontes; Play só após escolha |
| 5 | E08: diagnósticos/export/backup sem Drive paralelo; E09 somente trailers | Codex Alto em trilhas separadas | Sem dados sensíveis; produção HOLD; chaves não publicadas |
| 6 | E11: um só monitor de Original+ARC+ReSeq e ledger de PR/build QA | Codex Alto na implantação única, Actions na execução | PR e artefatos de QA, sem auto-merge/release/OTA |
| 7 | E10: regressão integrada e homologação física | Commander/Gradle/ADB; ChatGPT revisão | PASS real em Fire/Shield, assinatura, compatibilidade, rollback; autorização final humana |
| Opcional | E06-LAB TVDB alternativo, E02-LAB Usenet, melhorias de DAN TV+ | Trilha independente com gate de produto | Sem bloquear 2.1.0, sem comprometer fontes/artefatos homologados |

**Retorno obrigatório de cada executor:** `(1) Feito com arquivos/SHA; (2) Resultado por teste PASS/FAIL/NÃO TESTADO; (3) evidência e riscos; (4) SCRIPT DE RETORNO AO CHATGPT; (5) SCRIPT DA PRÓXIMA SEQUÊNCIA`. Falha/HOLD impede avançar automaticamente. Um único escritor por módulo; Gemini só leitura/parecer/documentação.

## 5. PRECHECK, CRITÉRIOS TÉCNICOS E PROIBIÇÕES

- Antes de código: ler `D:\APK\AGENTS.md`, `GEMINI.md`, plano/matriz, verificar Git HEAD/status/worktrees/processos e baseline 2.0.2/1644. Não usar worktrees históricas apagadas. Fonte oficial relatada: `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`; revalidar. Assinaturas e OTA antigo intactos.
- Todo patch em branch/worktree **isolada**, com dono único e contrato de arquivos. Fazer testes focados + regressão da integração, build mecânico e smoke real quando a UI/player mudou. Nunca chamar build de aprovação visual.
- **Preservar** Exo/MPV, Dolby TrueHD/DTS-HD/DTS:X/Atmos/DV, REMUX, perfis/PI​Ns, Scout existente, Supabase/RLS, identidade DanPlus, dados, histórico, ATV/Dpad, fonte/launcher e códigos de assinatura. Nada de fallback que ignore prioridade REMUX.
- **Não programar agora**, não mudar GitHub, releases, APKs, OTA, versionCode, assinaturas, chaves, produção Supabase ou DAN TV+ nesta auditoria.
- GPLv3 do ReSeq e atribuições de upstream precisam ser respeitadas se qualquer código for adaptado; confirmar compatibilidade de licença/dependências. Não carregar credenciais em relatórios ou arquivos públicos.
- Não inventar métricas de CPU/RAM/frames, builds ou progressos. Sem screenshots pareados, estética é parecer arquitetural. Sem teste real em Fire/Shield, comportamento é **NÃO TESTADO**.

## 6. FONTES E PONTOS DE VERIFICAÇÃO

- ReSeq release RC3: https://github.com/abbaskip/Nuvio-ReSeq/releases/tag/v1.0.0-rc.3
- ReSeq RC3.1: https://github.com/abbaskip/Nuvio-ReSeq/releases/tag/v1.0.0-rc.3.1
- ReSeq código/tag: https://github.com/abbaskip/Nuvio-ReSeq/tree/v1.0.0-rc.3.1
- ReSeq TV Mix: https://github.com/abbaskip/Nuvio-ReSeq/blob/dev/TV_MIX.md
- ReSeq monitor: https://github.com/abbaskip/Nuvio-ReSeq/blob/dev/docs/UPSTREAM_SYNC.md
- ReSeq pipeline: https://github.com/abbaskip/Nuvio-ReSeq/blob/dev/docs/BUILD_PIPELINE.md
- ReSeq Calendar Views: https://github.com/abbaskip/Nuvio-ReSeq/blob/v1.0.0-rc.3.1/app/src/main/java/com/nuvio/tv/livetv/calendar/CalendarViews.kt
- ReSeq FlickPick: https://github.com/abbaskip/Nuvio-ReSeq/blob/v1.0.0-rc.3.1/app/src/main/java/com/nuvio/tv/ui/screens/flickpick/FlickPickScreen.kt
- DanPlus Calendar: https://github.com/danilopagotto82/DanPlus/blob/main/app/src/main/java/com/nuvio/tv/ui/screens/calendar/CalendarScreen.kt
- DanPlus Scout: https://github.com/danilopagotto82/DanPlus/blob/main/app/src/main/java/com/nuvio/tv/ui/screens/scout/ScoutScreen.kt
- DanPlus Master Plan: `DANPLUS_2_1_0_PLANO_MESTRE_ATUALIZADO_20261122.md` (cópia local da conversa; não persistida no GitHub)

**RESULTADO FINAL:** auditoria documental/código **REALIZADA**; itens integrados no planejamento, sem alterá-los na matriz oficial das 146 decisões. Repositórios e aparelhos **NÃO ALTERADOS**; testes/build/smoke/UX pareada **NÃO TESTADOS**. Próximo passo seguro: E00 somente leitura e inventário dos RSQ, depois proposta de primeiro patch com gate humano.
