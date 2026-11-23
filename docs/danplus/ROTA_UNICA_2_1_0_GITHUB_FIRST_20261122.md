# DANPLUS 2.1.0 — ROTA ÚNICA GITHUB-FIRST / E00 REMOTO
**Data:** 22/11/2026 (Brasil) · **Estado:** auditoria documental e estática, SEM patch de APK, sem Gradle, sem deploy, sem merge, sem release.

## 1. Fontes e precedência
- Repositório oficial: `danilopagotto82/DanPlus`, `main` SHA `3715ee0de8345d617f48c2da7271716ed501dc5c`, confirmado por leitura GitHub nesta rodada.
- Ler `GEMINI.md`, `docs/danplus/PLANO_DANPLUS_2_1_0.md`, `docs/danplus/PROXIMA_VERSAO_PRONTIDAO_20261119.md`, `docs/danplus/INDICE_MUD.md` e `docs/danplus/gemini-planejamento/DP_CODEX_00..10` conforme aplicável.
- Fontes de planejamento local / arquivo de projeto lidas nesta rodada: `DANPLUS_2_1_0_PLANO_MESTRE_ATUALIZADO_20261122.md` e `DANPLUS_2_1_0_ROTEIRO_EXECUCAO_COM_SCRIPTS_20261122.md`. **Não foram encontradas como arquivos do GitHub oficial.** ATUALIZAÇÃO DE RECUPERAÇÃO (22/11): matriz e auditoria foram LOCALIZADAS e ABERTAS integralmente na Biblioteca do projeto DaNPllus e copiadas como texto extraído para `docs/danplus/fontes-recuperadas/` nesta branch. Matriz: 146 IDs únicos, contagens de votos confirmadas; ReSeq: RSQ-01..20 todos presentes. Esses dados são documentos de decisão, NÃO prova de implementação no código, não substituem E00-L.
- Base protegida nos documentos: APK DanPlus 2.0.2, versionCode 1644, Fire `com.danexus.tv.fire.debug`, Shield `com.danplus.shield`; fonte local indicada `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`; APKs protegidos `D:\APK\DanPlus_2_0_2_HOMOLOGADO`; repositório OTA legado não renomear/não apagar. Estes CAMINHOS não foram inspecionados agora.
- GitHub: 403 de escrita foi resolvido, com branch e commit de prova `7e34af09` na branch `chore/wetrakr-backend-prep-20261122`. Nunca escrever diretamente na main sem gate.
- Commander: dispositivo DESKTOP-5P89BAD aparece OFFLINE nesta rodada. AGENTS.md local, Git dirty, assinatura, hashes APK, SDK, equivalência binário-fonte e smoke permanecem NÃO TESTADOS.

## 2. Contabilidade anti-duplicação
- Matriz de decisões documentada: **146 IDs = 105 APROVAR + 23 ADAPTAR + 8 INVESTIGAR + 4 MANTER + 5 NEGAR + 1 NÃO RECOMENDAR.** Não são 146 patches; parte é vínculo/histórico. Proibido reverter B2-08 por B2-12/13, portar negativas ou promover INVESTIGAR sem evidência.
- Quatro extras prévios: `DP21-BRAND-01` (apenas marca visível, preservar créditos/IDs), `DP21-LARGE-01` (REMUX intacto), `DP21-UPSTREAM-CI-01` (monitor Original+ARC), `DP21-BACKDROP-VITRINE-01` (carregamento de arte).
- Extras posteriores: `DP21-GRID-META-01` (séries: ano início–fim, episódios confiáveis, classificação etária local, status honesto, sem nova chamada a cada DPAD); `DP21-PAUSE-CREDITS-01` (somente Direção e Elenco); `RSQ-01..20` (auditoria ReSeq / FlickPick / Advanced Discover / TV Mix; não substituir Scout ou Calendário; sem TV ao vivo/EPG/Sports Hub/Multiview, que pertencem a DAN TV+).
- Pedido posterior da conversa: `DP21-EMCENA-01` — piloto E05C de “Em Cena”: somente badge na tela Detalhes quando houver índice de pessoas/cena **validado para o título e a edição/linha do tempo**, correlacionado à miniatura Seekr; SEM IA facial no player, SEM promessa de 100%, SEM nova engine; piloto não bloqueia 2.1.0 se não conseguir base de dados confiável.
- Pedido posterior da conversa: `WETRAKR-BACKEND-01` — Client ID público centralizado no Supabase STAGING (cadastro e leitura anon relatados/validados em sessão anterior); Android ainda não consome o registro; fluxo local existente por perfil via OAuth-device; Client Secret **distinto** jamais deve ser incorporado à APK, GitHub ou configuração pública. Não reproduzir credenciais em documentação. **Importante:** o cadastro do Client ID no Supabase não transfere automaticamente histórico/progresso ao Supabase; no fluxo já existente, os dados são sincronizados com WeTrakr após autenticação individual. Backup próprio no Supabase, caso venha a existir, exigirá escopo, consentimento, RLS e testes separados.
- MUD/REQ16/BUG já cobertos: LOGO-PTBR-01, BUG-BACKDROP-01, BUG-THUMB-01, Seekr, Indicações, Vitrine/Glass, Scout, Calendário, trailers PT-BR, backup e estatísticas. Exigir um dono por componente e mapear `JÁ EXISTE / PARCIAL / AUSENTE / INVESTIGAR / NÃO APLICÁVEL`.

## 3. Achados atuais da fonte GitHub (não são PASS funcional)
- `app/src/main/java/com/nuvio/tv/data/wetrakr/` e `core/di/WeTrakrModule.kt`: autenticação WeTrakr local, sincronização, quota, Client ID via BuildConfig ou preferência local; **fetch do Supabase para o ID ainda pendente**.
- `domain/model/MetaPreview.kt`: possui `seasonCount`, `status`, `ageRating`, mas nenhum total explícito de episódios; `ui/components/DanPlusFocusDock.kt` mostra ano/duração/rating, falta provar ligação com a Grade lateral real. Não implementar contagem nova sem contrato de E06.
- `ui/screens/player/PauseOverlay.kt` + `PauseCredits.kt`: apresentadas categorias Direção/Elenco, sem grupo Writer; QA físico pendente; não fazer patch hipotético.
- `ui/screens/scout/ScoutScreen.kt` e `core/scout/ScoutCatalogProvider.kt`: já há seleção `surprise`/Surpreenda-me; ReSeq FlickPick deve aproveitar motor existente, não substituí-lo.
- `core/calendar/`: serviços TMDB, Trakt, TVmaze, TVDB já existem; preservar apresentação local e deduplicar dados.
- GitHub `main` árvore consultada: **não há `.github/workflows/`**, portanto CI monitor Original+ARC+ReSeq não está implantado.
- Supabase STAGING consultado na sessão anterior tinha funções de Indicações, relatórios e login implantadas; **não equivale a aprovação ou smoke end-to-end em Android**. Documentos antigos dizendo “backend não implantado” precisam ser reconciliados serviço a serviço sem tocar produção.

## 4. Sequência antirretrabalho (dono único; sem compilação até terminar codificação)
**E00-G (agora via GitHub):** inventário estático de IDs/arquivos/diff e upstream; classificar gaps; preparar scripts/casos de teste; não marcar equivalência de APK como PASS.
**E00-L (Commander obrigatório):** PC online, ler `D:\APK\AGENTS.md`; status/HEAD/worktrees/processos, hashes APK, certificado/alias, caminho fonte, YouTube fallback e mpv Maven, comparar 2.0.2 original; evitar limpeza/reset. Se FAIL, parar antes de patches críticos.
**E01 (condicional):** somente reproduzido crash Compose Detalhes; não recodificar crash histórico sem prova.
**E02 → E04 → E03:** fontes/retry/cancelamento → buffer/ranges/Fire memória → formatos/áudio passthrough; Codex único escritor; Exo/MPV e REMUX intactos.
**E06 → E05:** contrato de identidade/progresso/calendário/episódios primeiro; depois UX, Focus Dock, série na Grade, backdrops, Seekr miniatura e pausa. E05C “Em Cena” como prova de conceito NÃO bloqueante e SEM IA obrigatória.
**E07A → E07B:** auditoria de marca/textos PT-BR (Gemini leitura; patch de strings isolado) → legendas/Scout/FlickPick/PIN/UX (Codex); não copiar Traduções negadas, nem um segundo Scout.
**E08:** Supabase/RLS multi-conta, Indicações, relatórios, WeTrakr Client ID remoto e cache Android; Client Secret separado e fora de clientes; **deploy real e migrations apenas com gate próprio**.
**E09:** trailers PT-BR/YouTube/cache, missão independente de player e staging; evitar quotas e segredo na APK.
**E11:** monitor de upstream GitHub Actions (Nuvio Original + ARC, ReSeq como observação), somente dry-run após gate; ledger, PR draft, sem autopatch/automerge/OTA.
**E10:** somente depois de todos os componentes codificados e gates locais PASS: testes focados Gradle, **uma campanha de builds finais** para Fire/Shield, smoke físico/DPAD/REMUX/som/legendas/OTA, certificados e revisão de diffs. Nada de APK 2.1.0 antecipada.

## 5. O que dá e o que não dá em GitHub
**GitHub AGORA:** ler/comparar fonte e upstream; inventariar IDs; planejar testes e cache; revisar Gradle/strings/contratos; escrever documento de coordenação em branch; preparar patches para revisão. Código Kotlin/Compose poderá ser editado em branch por Codex após E00/gate, mas sem testes de compilação até fase acordada.
**Requer Commander/PC:** inspecionar `AGENTS.md` e worktrees locais; validar correspondência APK-fonte, Gradle/JDK/SDK, certificados Fire/Shield, arquivo protegido; testes em hardware, ADB, player/REMUX, logs reais e instalação in-place/OTA.
**Não aprovar sem evidência:** IA para identificação em cena no título/edição, substituição integral de fork, ASR automático, migration de produção, segredo no APK, CI com auto-release, marca/IDs técnicos alterados, publicidade de PASS em UI sem screenshots físicos.

## 6. Estado desta rodada
- PASS: leitura GitHub main / plano e roteiro disponíveis, branch documental isolada; diagnóstico parcial de código e ausência de workflows.
- NÃO TESTADO: build, Kotlin, ADB, dispositivos, assinaturas, performance, crashes, QA visual, deploy de WeTrakr no APK, equivalência APK-fonte, confronto completo **cada requisito × implementação** (apesar das fontes documentais agora recuperadas).
- Sem side effects no código Android, `main`, APK, Supabase, releases, certificados, usuários ou dados.

**Primeira ação exata depois deste inventário:** quando Commander estiver online, rodar E00-L exclusivamente leitura; conferir AGENTS.md e baseline 2.0.2. Corrigir bloqueio de equivalência antes de iniciar E02/E04; depois autorizar cada megalote com um único executor e sem APK antes da codificação completa.
