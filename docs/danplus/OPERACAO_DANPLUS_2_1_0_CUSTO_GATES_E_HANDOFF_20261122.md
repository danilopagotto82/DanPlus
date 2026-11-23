# DanPlus 2.1.0 — Operação única, custo controlado e ZERO compilação pré-freeze
**Data:** 22/11/2026 (BRT) · **Estado:** planejamento operacional verificado por fontes; **não é autorização de patch Kotlin, build, deploy, instalação, merge ou release**.
**Ramo documental:** `docs/danplus-210-github-first-20261122`. **Main de referência:** `3715ee0de8345d617f48c2da7271716ed501dc5c` (revalidar se houver mudanças).

## 22/11 — TRAILER-DETAIL-FALLBACK-01 (correção isolada já codificada em branch)
**Sintoma relatado:** trailers de filmes como *Minority Report* e *A.I. – Inteligência Artificial* reproduzem no pôster expandido, porém não aparecem/iniciam na tela Detalhes.

**Causa comprovada no código, sem runtime:** `MetaDetailsViewModel.fetchTrailerUrl` usava somente `TrailerService.getTrailerPlaybackSourceFromTmdbId(... brazilianPortugueseOnly = true)`; já `TrailerPreviewLookup.lookupTrailerPreview` do pôster usa provider configurado/idioma alternativo e IDs YouTube do próprio título. O primeiro caminho pode retornar null embora o segundo reproduza. **Não foi confirmada a disponibilidade PT-BR concreta desses dois títulos no TMDB.** Auto-play em Detalhes ainda depende de `TrailerSettingsDataStore.enabled` (padrão false, atraso padrão 7 s).

**Patch preparado e salvo:** [PR #1 (DRAFT — NÃO MERGEAR)](https://github.com/danilopagotto82/DanPlus/pull/1), branch `fix/danplus-210-detail-trailer-fallback-20261122` a partir do SHA `3715ee0de8345d617f48c2da7271716ed501dc5c`. Alterações apenas em `TrailerService.kt` e `MetaDetailsViewModel.kt` e testes `DetailTrailerSourceSelectionTest.kt`/`TrailerServiceUseTrailersGateTest.kt`:
- PT-BR continua com precedência absoluta;
- na ausência de fonte PT-BR reproduzível, passa a reutilizar `lookupTrailerPreview` (mesmo caminho de fallback do card), com IDs normalizados do título, fonte de vídeo e áudio;
- respeita `TmdbSettings.useTrailers` também no fallback, preservando a desativação definida pelo perfil;
- não altera autoplay, duração, player, escolha de codec, runtime, Supabase, chaves, versão, assinatura ou 2.0.2.

**Verificações feitas:** diff GitHub de 4 arquivos no escopo; branch isolada; quatro testes Kotlin novos escritos (**NÃO EXECUTADOS**); nenhum Gradle/build/APK. **Não declarar BUG RESOLVIDO NA TV** até E10 com Minority Report, A.I. e um filme com trailer PT-BR; testar modo auto ligado/desligado, fonte original, trailer do addon, falha temporária e sem trailer; confirmar retorno da navegação e áudio Fire/Shield. Antes do merge/integrar com lotes maiores, atualizar branch de integração a partir da fonte homologada após C01.

**Limite:** se o TMDB não fornecer referência, o YouTube exigir login, as URLs expirarem ou o usuário tiver desabilitado trailers, nenhum fallback pode garantir reprodução. E09 (dublagem/legenda PT-BR/cache backend) permanece missão maior, separada deste bug.

---
## E00-G — auditoria de código realizada, sem compilação (22/11)
- **Relatório:** [E00_GITHUB_RESULTADOS_PRIORIZADOS_20261122.md](E00_GITHUB_RESULTADOS_PRIORIZADOS_20261122.md).
- **Matriz com evidência individual:** [E00_GITHUB_AUDITORIA_EVIDENCIAS_166_20261122.csv](E00_GITHUB_AUDITORIA_EVIDENCIAS_166_20261122.csv), **166/166 IDs rastreados** e **62 caminhos verificados**. Estados de base parcial não significam PASS funcional.
- **Equivalência estática:** novo GitHub/main vs. commit legado 2.0.2: **5.133 blobs iguais, 3 arquivos modificados, 29 arquivos só no novo, 7 só no legado**. Somente arquivos de código modificados: `app/build.gradle.kts` e `InAppYouTubeExtractor.kt` (fallback configurável; sem chave em Git). Prova de linhagem, NÃO de APK reproduzível.
- **Gaps encontrados:** failover só percorre candidatos posteriores ao atual; idioma do logo pode cair em inglês em vez do original oficial; grade de séries sem contador no Focus Dock; WeTrakr ainda sem consulta remota do Client ID. Há estruturas que já existem (Scout/Surpresa, fontes de legenda via celular, pausa com Direção/Elenco), evitar recodificar.
- **Check de proteção:** comparação entre base e branch confirmou **somente arquivos `docs/danplus/` alterados, zero arquivos `app/`**; `main` não alterada.
- **Commander (consulta de status):** DESKTOP-5P89BAD **offline**; C01, assinaturas, AGENTS.md local e smoke NÃO TESTADOS. Não transpor C01 por inferência.
- **Autorização atual cumprida:** E00-G auditoria estática e documentação. Código Kotlin/Compose 2.1.0 NÃO IMPLEMENTADO; E10 ainda não iniciado.

---
## PRIORIDADE OPERACIONAL — GITHUB PRIMEIRO / COMMANDER SOMENTE EXCEÇÕES
**Decisão do proprietário em 22/11/2026:** realizar no GitHub tudo o que for tecnicamente seguro, verificável e reversível. Acionar Desktop Commander apenas quando for impossível concluir com evidência equivalente no GitHub; **não usar Commander por conveniência ou como executor permanente**.

### Fases que devem ficar no GitHub
- **E00-G:** matriz 146 + 20 RSQ + MUD/REQ16/DP21, inventário por símbolo/arquivo, comparação de commits upstream, casos de teste, contratos, deduplicação e checklist. Usar conector GitHub para leitura e escrita documental em branch isolada.
- **E01–E09 e E11 após E00-L PASS / autorização do lote:** Codex é autor único Kotlin/Compose em **branch de integração separada da documental**, usando repositório GitHub conectado quando essa forma de edição estiver disponível; GitHub recebe commits, revisões e diffs. Gemini apenas audita grande volume em leitura; ChatGPT aprova gates e faz revisão. Nada de ida/volta de arquivos manuais pelo PC, nem alterar main.
- **Verificações sem compilação no GitHub:** leitura/símbolos/imports/recursos, revisão de diffs e segredos, testes Kotlin **escritos e não executados**, casos negativos, reconciliação por ID, comparação de fluxo com código upstream, checklist por commit. **Sem executar Gradle, CI de build, ADB nem testes de runtime antes do FREEZE.**
- **E08 e E09:** código/contratos SQL/Edge Function preparados no GitHub, testes de autorização em LAB Supabase somente com gate específico. Sem credenciais ou tokens no Git e sem deploy de produção.
- **E11:** preparar monitor determinístico Original + ARC + ReSeq somente como arquivos/configuração desativada até gate de CI. GitHub Actions não é prova de compatibilidade com a TV; não ativar agenda/compilação por engano.
- **E10:** depois do FREEZE, avaliar GitHub Actions para testes/builds **sem assinatura de distribuição** somente se houver ganho concreto e orçamento/limites confirmados; não disponibilizar certificados privados no GitHub como atalho. O fechamento assinado/in-place fica no PC quando necessário.

### Commander: SOMENTE DOIS GATES DE PC
1. **C01 — PRÉ-CHECK LOCAL OBRIGATÓRIO, UMA SESSÃO CURTA APÓS E00-G:** reconectar DESKTOP-5P89BAD e ler `D:\APK\AGENTS.md`; conferir worktree, conflitos/estado sujo, material real 2.0.2, hashes de APK, identidade de fonte, signing/OTA, Java/Gradle/SDK e arquivos privados que não existem no Git. É bloqueador antes de alteração Android arriscada; nunca enviar chaves ou dados de perfil à conversa/GitHub. Se PC estiver offline, continuar somente E00-G, documentos, testes escritos e revisão; não marcar C01 PASS nem ultrapassar gate de código de risco.
2. **C02 — COMPILAÇÃO E HOMOLOGAÇÃO FINAIS, UMA CAMPANHA APÓS FREEZE:** recuperar branch aprovada sem sobrescrever alterações locais; executar tarefas Gradle/testes necessários, comparar assinaturas e package IDs, obter APKs compatíveis Shield/Fire e validar no hardware real via ADB/screenshot, REMUX/áudio/Seekr/UI/OTA. Retornar dispositivo ao estado inicial quando apropriado. Só fazer deploy/release/OTA após autorização própria.

**Exceções intermediárias permitidas:** Commander só para incidente demonstrado que dependa de runtime local, dispositivo, assinatura ou arquivo inacessível remotamente, com diagnóstico específico e ação mínima. Se houver erro do player que só se manifesta no aparelho, pode haver coleta pontual de log antes de E10; isso NÃO é compilação intermediária. Nunca tornar C01 ou C02 uma rotina diária.

### Rotina econômica e trava anti-retrabalho
- Uma única fila `ID → arquivo/estado → dono → contrato → testes futuros → evidência → gate`, atualizada na branch com checkpoint após cada lote.
- Um único autor de Kotlin por vez; agrupar alterações pelo componente. GitHub é o local central de código, documentos e revisões. Não alternar GitHub ↔ PC para cada patch.
- **FREEZE não é PASS:** todos os contratos e diffs revisados, sem pendências não explicitadas; a execução de testes Kotlin/Gradle somente na E10 e o smoke físico continuam necessários.
- Sem estimar custos de GitHub Actions, Codex ou quotas de IA sem fonte/painel atual; preferir ferramentas mecânicas e executar lotes amplos para reduzir recargas de contexto.

### Fila operacional já criada (sem inventar implementação)
O arquivo `docs/danplus/MAPA_E00_GITHUB_146_RS20_PENDENTES_20261122.csv` foi gerado nesta branch com **166 IDs únicos** (146 da matriz oficial + 20 ReSeq), cruzados com os responsáveis do roteiro. **Todos os itens ativos permanecem como PENDENTE E00-G até inspeção símbolo/arquivo e código real**, não PASS. Os seis excluídos permanecem fora da programação; os oito INVESTIGAR não estão autorizados para patch.

### Próxima ação disponível agora, sem PC
**E00-G (somente leitura de código):** preencher matriz `ID | decisão | status comprovado no GitHub | arquivo/símbolo | vínculo | dono E__ | teste futuro | gate` antes de liberar implementações. **Não acionar Commander** enquanto a auditoria remota puder avançar; C01 é chamado quando faltar exclusivamente evidência de PC/2.0.2.

---
## 0. Fonte de verdade e regras que prevalecem
1. Runtime, PC, SHA, APK 2.0.2 e testes reais, quando disponíveis; depois Git; depois fontes canônicas; por último registros históricos e conversas.
2. Ler os cinco documentos preservados em `docs/danplus/fontes-recuperadas/`: `DANPLUS_2_1_0_MATRIZ_146_DECISOES.csv`, `DANPLUS_2_1_0_AUDITORIA_RESEQ_SENIOR_20261122.md`, `DANPLUS_2_1_0_PLANO_FINAL_SEM_BUILDS_20261122.md`, `DANPLUS_2_1_0_SCRIPTS_POR_IA_SEM_BUILD_20261122.md`, `DANPLUS_2_1_0_CONTINUIDADE_MELHORIAS_EM_CENA_20261122.md`. Conferir também `GEMINI.md`, `docs/danplus/PLANO_DANPLUS_2_1_0.md` e `PROXIMA_VERSAO_PRONTIDAO_20261119.md`.
3. **O PLANO_FINAL_SEM_BUILDS é mais recente que os roteiros antigos no ponto de colisão:** sequência **E06 → E07B-LÓGICA → E05-UI**; **ZERO Gradle/compilação/testes compilados/lint via Gradle** em E00–E09/E11; uma campanha E10 depois do freeze. Os demais documentos são complementares; não apagar votos da matriz.
4. A matriz contém 146 decisões documentais (105 aprovar, 23 adaptar, 8 investigar, 4 manter, 5 negar e 1 não recomendar). **Aprovação de produto não equivale a patch ou teste PASS.** Os 20 RSQ são catálogo complementar, não 20 patches; REQ16, MUD, DP21, BUG/LOGO também são vínculos, não novos motores.
5. **Sem compilação ANTES de todos os patches**, inclusive `gradlew test`, `lint`, `compile`, `assemble`, `bundle` ou builds de Actions. Escrever testes agora, executar na E10. Validações estáticas permitidas: busca, leitura de dependências, revisão de imports/assinaturas, `git diff --check`, parser XML sem Gradle, verificação de schema estática e revisão de secrets; nunca alegar PASS Kotlin/runtime antes de executar.
6. **Uma campanha final**, não promessa de sucesso na primeira tentativa. A compilação final naturalmente gera **APKs separadas para Shield arm64 e Fire armeabi-v7a** se confirmar os variants: `app/build.gradle.kts` prevê `full` e `fullFire` sob `buildingUnifiedOta`, além de build type `fireOta`. Comandos Gradle e credenciais REAIS devem ser descobertos/confirmados no E10; jamais inventar task ou usar assinatura incorreta.

## 1. Organização física e isolamento
- **Proteção intocável:** APK 2.0.2/versionCode 1644; Shield `com.danplus.shield`; Fire `com.danexus.tv.fire.debug`; certificados/fingerprints; histórico dos perfis; OTA de `danilopagotto82/DANPLUS-Shield-Arc31-Private`; sem reset/clean/force push.
- **GitHub remoto:** o 403 foi RESOLVIDO para branch e commit; repositório `danilopagotto82/DanPlus`, `main` não alterada pela operação documental. Não usar a branch de docs para implementar Kotlin.
- **PC informado nos planos:** `DESKTOP-5P89BAD`; pasta `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`; binários em `D:\APK\DanPlus_2_0_2_HOMOLOGADO`. **Caminhos e estado local NÃO verificados nesta rodada; Commander aparece OFFLINE.**
- Após E00-L PASS, propor **uma worktree limpa isolada** para 2.1.0 com branch de integração, vinculada ao SHA acordado e checkpoint. Nunca assumir que PC já está sincronizado; nunca sobrescrever trabalho sujo. PR draft somente após diff, sem merge automático.
- **Conflito de editor:** só UM Codex escreve Kotlin/Compose; Gemini e ChatGPT revisam; nenhum agente atua sobre os mesmos arquivos em paralelo. Ao fechar um megalote: commit local/branch, mapa de IDs, arquivos, diff, provas estáticas, testes a executar no E10 e rollback.

## 2. Custo / produtividade: regra de despacho
| Tarefa | Executor mais econômico | Esforço de IA | Regra de escalonamento |
|---|---|---|---|
| Coordenar, deduplicar, definir contrato e gate | ChatGPT normal, GPT-6, Alto | Curto, no começo/fim dos marcos | Não ficar monitorando shell/Gradle |
| Auditar strings, pastas, comparações Original/ARC/ReSeq, screenshots fornecidos | Gemini/Antigravity, modelo rápido disponível | Leitura de alto volume | Relatório por arquivo/linha; SEM editar |
| Git status, hash, diff, `diff --check`, parser XML, testes mecânicos futuros | Commander/terminal/PowerShell | Zero tokens de agente em execução mecânica | Requer PC online; ChatGPT analisa apenas saída final |
| Kotlin/Compose/player, contratos, SQL de LAB, testes escritos | Codex, modelo de programação mais capaz disponível, raciocínio Alto | Concentrado por megalote, 1 autor | Não chamar para tarefas meramente documentais |
| Consulta GitHub, commit em branch, diff/PR e monitor inativo | Conector GitHub/GitHub Actions | Baixo custo de IA | CI/schedule e builds de Actions desativados antes do gate |
| Migração/deploy Supabase | Ferramenta Supabase após revisão | Gate humano próprio | Não misturar com patch Android; preservar staging/prod |
| Build/testes/smoke E10 | Gradle, ADB, apksigner, terminal | Ferramentas nativas; IA só se FAIL | Não criar APKs por tela ou correção mínima |

**Sem promessa de limites, cotas, horas ou preços numéricos** sem painel de cobrança/limites verificado. Custo adicional de agentes e CI deve ser observado quando forem realmente utilizados; economizar chamadas, não cobertura de teste.

## 3. Ordem e portas de qualidade: UM DONO POR COMPONTENTE
| Fase | Entrega exigida | Ferramenta | Gate / proibição |
|---|---|---|---|
| **E00-G** agora | Ler fontes no GitHub e mapear **cada ID** a JÁ EXISTE/PARCIAL/AUSENTE/INVESTIGAR/DUPLICADO/NÃO APLICÁVEL, com arquivo/função e prova; confrontar upstream, contratos e UI | GPT-6 Alto; Gemini para inventário volumoso | Somente leitura, sem inferir runtime da TV |
| **E00-L** obrigatório | Ler `D:\APK\AGENTS.md`; PC, worktree, Git sujo/remotos/processos; SHA da fonte 2.0.2 vs binário, assinatura Fire/Shield, Gradle/SDK, mpv, fallback YouTube, OTA | Commander/PowerShell leitura; Codex só se diagnóstico complexo | Se correspondência, signing ou P0 sem solução: **HOLD** antes de patches críticos |
| **E01** condicional | Crash Detalhes/ANR ou redirecionamento Plex/Emby com token **somente se comprovado** | Codex Alto | Não refatorar sem reprodução/evidência |
| **E02** | Fontes, retries/expiração/Debrid, cancelamento, orçamento total, preservar posição e fonte manual | Codex Alto | Contrato de stream congelado; casos 0/7 fontes e cancelamento preparados |
| **E04** | Range/buffer/cache/seek/Fire 32-bit; arquivos REMUX sem downgrade | Codex Alto | Não tocar decisão do decodificador ou bitstream; contrato de E02 respeitado |
| **E03** | Exo/MPV, áudio HD/TrueHD/DTS, HDR/DV, HLS/DASH, Hz/ARC | Codex Alto | Preservar Exo passthrough HD, MPV PCM ≠ bitstream; sem transcodificar |
| **E06** | Dados e contratos de séries/episódios/Trakt/WeTrakr tracking/Continuar/Calendário; status, anos, classificação e contagem válida | Codex Alto | Não alterar UI; entregar DTOs e callback estáveis para E07B e E05 |
| **E07B-LÓGICA** | Scout/Descubra-me aproveitando `surprise`, FlickPick/Advanced Discover/TV Mix, PIN, perfil, listas/filmes vistos, legendas funcionais | Codex Alto | Um único motor Scout e seletor de fontes, contratos de UI congelados antes de E05 |
| **E05-UI** | **ÚNICA passagem visual:** Home/Vitrine/Glass, logos PT-BR→original, backdrops, Grade de séries, Focus Dock, Calendário **visual DanPlus + só filtros/Hoje úteis do ReSeq**, Pausa Direção/Elenco se bug, Seekr moldura | Codex Alto + Gemini auditoria visual | Sem segundo loader/motor, manter sinopse e DPAD; screenshot real só no E10 |
| **E05C piloto** não bloqueante | “Em Cena” apenas se índice título+edição+timecode validado com dados licenciados; badge condicional só após piloto; sem reconhecimento facial no APK | GPT arquitetura + Codex LAB | Se não houver dado e prova real, **NÃO EMBARCAR badge**; não atrasar 2.1.0 |
| **E07A** | Marca visível DanPlus/PT-BR; preservar nomes técnicos/créditos Nuvio/ARC, licenças | Gemini lê; Codex edita strings afetadas | Não fazer replace global nem retraduzir funcionando |
| **E08** | Indicações/relatos/backup/RLS, WeTrakr Client ID público remoto + cache/validação; proteção de tokens e consentimentos | Codex LAB + GPT auditoria de segurança | Supabase STAGING já tem Client ID público; Android ainda não lê; Client Secret separado NÃO entra APK/Git; **sem deploy produção** |
| **E09** | Trailer PT-BR dublado/legendado, fallback, cache TTL/backend, Minority Report | Codex Alto | Sem chave embutida; missão isolada |
| **E11** opcional | Monitor Original + ARC + ReSeq, ledger por SHA/id, relatório de diff | Codex depois de proposta GPT | Sem habilitar schedule, builds, auto-merge, auto-release ou OTA; não bloquear E10 se não essencial |
| **FREEZE** | 100% dos itens decididos têm classificação/prova ou HOLD explícito; todos patches concluídos; contratos fechados; diff limpo; testes escritos; sem segredos | GPT-6 Alto + inspeções locais estáticas | FAIL => corrigir código sem compilar e revisar novamente |
| **E10** final | Tasks reais Gradle; testes compilados; campanha de 2 APKs compatíveis; SHA/assinaturas/ABI/versionCode; smoke real Fire e Shield; comparação de screenshot e OTA | Commander+Gradle+ADB; Codex só no FAIL | Build PASS ≠ smoke PASS. Release/instalação/deploy apenas sob aprovação própria |

**Ordem de dependência que prevalece:** `E00-G → E00-L → E01? → E02 → E04 → E03 → E06 → E07B-LÓGICA → E05-UI → E07A → E08 → E09 → FREEZE → E10`. E05C é piloto condicionado e E11 é trilha opcional, ambos sem travar a versão estável. E08/E09 podem ser *analisados* em paralelo, mas **um único autor Kotlin por vez**.

## 4. Critérios de conclusão por megalote, sem compilação
Cada entrega precisa fornecer:
1. IDs cobertos / IDs deduplicados / itens INVESTIGAR ou NEGAR não alterados.
2. Arquivos exatos, commit SHA e diff; prova de que não tocou pacotes, assinaturas, banco, OTA.
3. Contratos de entrada/saída estáveis, efeitos por perfil, permissões, cache, offline, DPAD e regressões esperadas.
4. **Testes de unidade e regressão ESCRITOS, NÃO EXECUTADOS**; evidências de revisão estática com resultado verdadeiro; `git diff --check` quando branch local disponível.
5. Checkpoint de continuidade autossuficiente; ao haver sobreposição de arquivo, suspender e atribuir dono único, não abrir dois agentes.
6. Não criar APK; não mudar main, segredos, Supabase real, lançamentos, certificados ou aparelhos.

## 5. Portas finais de homologação e contingência
- E10 deve descobrir os nomes reais das tasks e condições dos flavors no ambiente local; registro de variant Fire `fullFire` existe só sob `buildingUnifiedOta`; usar propriedades reais, confirmar certificados e fingerprint contra 2.0.2.
- **PASS** por teste separado: compilação/testes (Kotlin), arquivos APK assinados, Fire DPAD/Classic, Shield Classic/V2, Busca/Biblioteca/Indicações, Scout/Calendário/Grade/Seekr, áudio/REMUX/seek, legendas, login e progresso, OTA in-place, fotos de tela antes/depois.
- Se compile/teste falhar no primeiro fechamento, **não gerar APK de tentativa e publicar**. Congelar a regressão, diagnosticar pontualmente, corrigir, repetir apenas o necessário, retestar regressão; documentar que a campanha exigiu outra execução. **Não alegar uma tentativa única quando falhou.**
- Se QA visual/áudio não tiver PASS real, versão NÃO homologada mesmo que Gradle seja PASS.
- Manter 2.0.2 como rollback, sem alterar produção, releases e OTA até decisão humana final.

## 6. Handoff e primeira ação real
**SCRIPT a executar primeiro, sem abrir Codex:** ChatGPT normal, GPT-6 Alto: ler a matriz 146, auditoria RSQ, plano final e scripts em `docs/danplus/fontes-recuperadas/`; fazer **E00-G por IDs** contra símbolos atuais GitHub, uma única matriz de deltas (não escrever Kotlin). Saída: `ID | dono | estado código | arquivo/símbolo | prova | contrato | teste futuro | próximo gate`; marcar dúvidas NÃO TESTADO.

**E00-L depois:** abrir Commander quando PC voltar online, realizar inventário local, preservar 2.0.2, decidir gate de início de Codex. Não use Commander offline como desculpa para inventar PC PASS. Não iniciar patch relevante de Kotlin antes da equivalência 2.0.2.

**Scripts completos para ChatGPT, Gemini, Codex e freeze E10** já preservados em `docs/danplus/fontes-recuperadas/DANPLUS_2_1_0_SCRIPTS_POR_IA_SEM_BUILD_20261122.md`. Primeiro leia `AGENTS.md` do PC quando acessível e `GEMINI.md` no GitHub. O resultado desta preparação é PLANEJAMENTO, não software implementado.

## 7. Estado verificado no momento da criação
- PASS documentais: 146 IDs e 20 RSQ presentes/íntegros; GitHub branch e gravação funcionais; `main` referência conforme leitura; fonte Gradle contém `full`/`fullFire` condicionais ao modo OTA; códigos Scout/Calendário/WeTrakr/Seekr existentes por inspeção estática.
- PENDENTE: E00-G completo requisito×símbolo; E00-L real; implementação E01..E09; piloto Em Cena; automação E11; backend WeTrakr integrado ao APK.
- NÃO TESTADO: Kotlin, Gradle, binário 2.1.0, proteção de dados em execução, QA visual, assinaturas atuais e smoke físico Fire/Shield/OTA.
