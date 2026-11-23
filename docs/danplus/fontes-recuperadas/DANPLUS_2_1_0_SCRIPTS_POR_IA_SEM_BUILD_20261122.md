# DANPLUS 2.1.0 — SCRIPTS OPERACIONAIS POR IA
**Versão:** 22/11/2026 · **Modo atual:** planejamento/inspeção. Nenhum script autoriza por si só alterações no projeto.

**Leia junto:** `DANPLUS_2_1_0_PLANO_FINAL_SEM_BUILDS_20261122.md`, plano mestre anterior, roteiro E00–E11 e matriz original. Onde esta atualização diverge dos scripts antigos, prevalece a regra **zero compilação até freeze** e **E06 → E07B-LÓGICA → E05-UI**. Não copiar os textos longos dos roteiros anteriores desnecessariamente se estiverem acessíveis no projeto.

---

## SCRIPT 1 — CHATGPT NORMAL · GPT-6 · RACIOCÍNIO ALTO
**Finalidade:** E00-A, auditoria remota e consolidação sem Commander.

```text
PROJETO DanPlus 2.1.0 — E00-A — RECONCILIAÇÃO REMOTA, SOMENTE LEITURA.
Ferramenta: ChatGPT normal, GPT-6, raciocínio Alto. Não usar Work, Commander, Codex nem Gradle nesta fase salvo necessidade comprovada.

Use como fontes os anexos DANPLUS_2_1_0_PLANO_FINAL_SEM_BUILDS_20261122.md e DANPLUS_2_1_0_SCRIPTS_POR_IA_SEM_BUILD_20261122.md; recupere também DANPLUS_2_1_0_PLANO_MESTRE_ATUALIZADO_20261122.md e DANPLUS_2_1_0_ROTEIRO_EXECUCAO_COM_SCRIPTS_20261122.md, além da matriz de 146 IDs e relatório RSQ separado quando disponíveis. Leia instruções AGENTS.md/GEMINI.md do repositório oficial pelo GitHub conectado, se acessíveis. NÃO substitua falta de acesso por afirmação de leitura.

Baseline relatada: DanPlus 2.0.2/1644; D:\APK\DanPlus_2_0_2_HOMOLOGADO; fonte D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119; GitHub danilopagotto82/DanPlus; OTA legado protegido. Não presumir estado local nem usar worktrees antigas.

TAREFA: deduplicar cada uma das 146 decisões, famílias MUD/REQ16/RS, pedidos DP21 e 20 RSQ contra o código público efetivamente acessível. Marcar JÁ EXISTE / PARCIAL / AUSENTE / INVESTIGAR / DUPLICADO / NÃO APLICÁVEL por arquivo, com ligação ao dono E00/E01/E02/E04/E03/E06/E07B/E05/E07A/E08/E09/E11/E10. Não misturar voto de produto com status de implementação. Conferir que os recusados não viraram tarefas. Reconhecer que a análise do código remoto não valida binário ou telas instaladas.

Priorizar: equivalência fonte sanitizada vs APK (fallback YouTube), credenciais Plex/Emby/redirect, libmpv, signing, versionCode, OTA; Grade lateral só séries; Pausa sem Writer; logo PT-BR→original; backdrop no primeiro foco; Seekr; Scout/Descubra-me sem segundo motor; Calendário DanPlus; REMUX íntegro. ReSeq RC3 e RC3.1 são fontes de ideias, não patches pré-aprovados. Sports/IPTV/EPG/Multiview são do DAN TV+, não DanPlus.

ENTREGA: (1) matriz única anti-retrabalho, riscos, provas e arquivo dono; (2) lista de contratos que E06/E07B devem fornecer antes da UI E05; (3) divergências DOCUMENTAIS, NÃO TESTADAS; (4) SCRIPT DE RETORNO AO CHATGPT; (5) script Gemini para inspeção local, SOMENTE LEITURA. NÃO programar, executar Gradle, alterar GitHub, gerar APK, instalar ou publicar. A autorização atual cobre SOMENTE planejamento.
```

---

## SCRIPT 2 — GEMINI / ANTIGRAVITY · MODELO RÁPIDO DE LEITURA
**Finalidade:** auditoria profunda, grande volume de arquivos, visual e nomenclatura; custo conservador.

```text
DANPLUS 2.1.0 — E00-B / AUDITORIA INDEPENDENTE SOMENTE LEITURA.
Ferramenta: Gemini/Antigravity; escolha o modelo rápido de leitura disponível, raciocínio padrão. Não abrir agente executor de código.

Antes de qualquer coisa, verificar se o diretório realmente existe, se há instruções locais AGENTS.md/GEMINI.md, Git HEAD/status e concorrentes. Fonte esperada D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119, não presumir que existe. Referência homologada: D:\APK\DanPlus_2_0_2_HOMOLOGADO. O projeto deve ser inspecionado SEM MODIFICAR QUALQUER ARQUIVO, sem atualizar dependências, sem downloads automáticos, sem compilar e sem abrir instalação.

Auditar em megalote único:
A. Comparar matriz 146/REQ16/MUD/RS/RSQ e nova consolidação com símbolos reais, já existentes, parciais, ausentes e duplicados.
B. Listar todos os lugares em que ARC/Nuvio aparece como marca própria, separar de IDs técnicos, créditos upstream/licença, provedores e URLs. Não fazer replace global.
C. Inspecionar dados de série (MetaPreview, Focus Dock, TMDB, anos, temporadas/episódios/status/classificação), Pausa (diretores/elenco), Backdrops, LOGO-PTBR, arte Home Vitrine/Glass, Indicações/Biblioteca/Descobrir, Seekr e DPAD. Indicar superfícies exatas e risco de requisições por foco.
D. Identificar reuse possível do ScoutEngine/ScoutViewModel/ScoutMixStore/Session, filtros ReSeq/FlickPick/Advanced Discover/TV Mix e modelo de episódios/Trakt; Calendário DanPlus antes de sugerir alternativa. PROIBIDO sugerir segundo motor ou portar Sports/IPTV/EPG.
E. Mapear dependências dos contratos entre E06 e E07B-LÓGICA que precisam ficar estáveis antes de E05. Auditar Fire vs Shield, libmpv, signing, fallback YouTube e segurança de tokens apenas por evidência acessível.

Entregar um relatório conciso por ID/subsistema com caminho, trecho/símbolo e status. Para UX, anotar o que é PARECER DE CÓDIGO versus SCREENSHOT REAL; não dizer que houve teste físico. NÃO editar XML, Kotlin, Gradle, Git, GitHub, Supabase, APK. NÃO executar nenhum gradlew, teste compilado, build ou deploy. Forneça SCRIPT DE RETORNO AO CHATGPT com achados e um script Codex E00 local sujeito a autorização.
```

---

## SCRIPT 3 — CODEX · PRECHECK LOCAL, SEM ESCREVER
**Finalidade:** antes de liberar qualquer implementação. Modelo de programação mais capaz disponível; raciocínio Alto.

```text
DANPLUS 2.1.0 — E00-C / PRECHECK LOCAL SEM PATCH.
Ferramenta: Codex, modelo de programação mais capaz disponível, raciocínio Alto. Usar shell/Git para inventário mecânico, sem comandar compilação.

Leia instruções locais na ordem (AGENTS.md, GEMINI.md, plano DanPlus 2.1.0, documentos consolidados) e verifique PC, worktree, Git status/HEAD, branches, worktrees, remotes, processos concorrentes, SHA e proteção do homologado. Não altere arquivos ou índices Git. A fonte esperada D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119 pode estar ausente: informe sem inventar.

Compare de modo somente leitura a fonte disponível com a referência 2.0.2/1644, commit histórico do OTA, dependência libmpv, compatibilidade de assinatura, IDs de pacote por flavor e fallback YouTube sanitizado. Verifique existência real da matriz de 146, documentos RSQ e se as decisões podem ser mapeadas a arquivos e funcionalidades reais. Nenhuma decisão marcada INVESTIGAR pode virar patch sem autorização técnica. Não tocar Supabase, GitHub remoto, aparelho nem CI.

Produza: estado real, riscos P0/HOLD, checkpoint reversível PROPOSTO (não executado), mapa de arquivos/donos, necessidades objetivas, artefatos ausentes e primeiro pacote elegível. Não executar Gradle/ktlint via Gradle/testes compilados/build/APK/ADB/instalação. NÃO EXECUTAR implementação ainda. Entregar SCRIPT DE RETORNO AO CHATGPT, com evidências, PASS/FAIL/NÃO TESTADO, e SCRIPT DE IMPLEMENTAÇÃO condicionado à autorização explícita.
```

---

## SCRIPT 4 — CODEX · IMPLEMENTAÇÃO INTEGRAL APÓS GATE
**Finalidade:** um único implementador Android com checkpoints por subsistema; **não colar até a autorização real de patches e E00 validado**.

```text
DANPLUS 2.1.0 — MISSÃO IMPLEMENTAÇÃO POR DEPENDÊNCIAS · SEM BUILDS INTERMEDIÁRIAS.
Ferramenta: Codex, melhor modelo de programação disponível, raciocínio Alto. VOCÊ É O ÚNICO ESCRITOR Kotlin/Compose/Player neste ciclo. Leia AGENTS.md/GEMINI.md, código real e os documentos oficiais, especialmente PLANO_FINAL_SEM_BUILDS_20261122 e matriz 146/REQ16/MUD/RS/RSQ. Não presumir que caminhos e arquivos da documentação ainda existem: confirmar. Não misturar trabalho de DAN TV+, NEXUS_7 e ÓRBITA.

GATE INICIAL: executar somente se E00 em leitura tiver PASS e autorização explícita para este escopo. Criar worktree/branch isolada e checkpoint SEM destruir o estado local; não tocar a main, secrets, signing, applicationId, OTA legado, banco de produção, aparelhos ou releases. Se houver conflito concorrente/alterações sujas irresolvidas/artefatos faltantes, retornar HOLD com diagnóstico. Não inventar requisitos da matriz ausente.

REGRA ABSOLUTA: NÃO executar qualquer Gradle que compile/teste/lint/assemble/bundle, nem gerar APK/AAB, nem instalar em aparelhos, até todos os patches aprovados estarem codificados e a fase FREEZE DE CÓDIGO ser formalmente aceita. Testes focados devem ser ESCRITOS agora e EXECUTADOS somente na fase E10, após freeze. Durante implementação usar somente leitura, buscas, revisão de diff, git diff --check e validações sintáticas que comprovadamente não executem compilação. Nunca marcar PASS de runtime sem executá-lo.

ORDEM OBRIGATÓRIA, um escritor por arquivo:
1. E01 apenas bugs P0 reproduzidos, incluindo credenciais em redirects se confirmado.
2. E02 resolve fontes/failover/timeout/cancelamento/retomada.
3. E04 resolve streaming Range/buffer/cache/memória para REMUX grande.
4. E03 resolve seleção de engine, formatos, bitstream HD, HDR/DV e taxa de quadros, preservando Exo/MPV e qualidade.
5. E06 entrega TODOS os contratos/modelos de identidade de episódio, contagem/status de séries, progresso/Trakt/Calendário sem editar a UI.
6. E07B-LÓGICA entrega o delta Scout/Descubra-me (reuso de surprise, filtros, mixes, fontes, PIN, série/episódio) e ajustes funcionais de legendas/perfis, congelando assinaturas/callbacks para E05.
7. E05 faz UMA passagem de UI por tela: Home/Vitrine/Glass, backdrops, logo PT-BR→original, grade lateral séries, Pausa somente se houver bug real, Calendário premium DanPlus, Focus Dock, Seekr moldura. Não criar segundo loader nem segundo Scout. Preservar sinopse, DPAD, memória Fire, Classic e aparência homologada.
8. E07A limpa somente marca visível/strings, mantendo package IDs e atribuições.
9. E08 valida em LAB relatórios, Silo, indicação, backups Supabase/RLS, sem migração ou deploy de produção.
10. E09 trailers PT-BR dublado/legendado/original e cache, com fallback YouTube sem segredo.
11. E11 cria somente arquivos para futuro monitor Original+ARC+ReSeq, ledger anti-duplicidade/PR draft, SEM ativar schedules, automerge, release, assinatura ou jobs que gerem APK antes do gate.

Para cada megalote: primeiro classificar JÁ EXISTE/PARCIAL/AUSENTE por ID, implementar somente DELTA necessário; preservar recusados, MANTER e itens INVESTIGAR; executar revisão estática e registrar arquivos alterados, diff, SHAs/commits locais, contratos, testes escritos NÃO EXECUTADOS, riscos e rollback. O próximo megalote não deve reabrir os arquivos do anterior sem evidência e parecer do coordenador. Pare apenas em conflito irreversível, segurança, decisão humana indispensável, FAIL estático ou risco de perda de trabalho; não pedir autorização a cada ação local segura.

Ao fechar TODOS os lotes: entregar um inventário de requisitos cobertos, matriz por arquivo/dono, diff integral, ausência de secrets, testes a executar depois, pendências reais e lista exata de tasks Gradle a descobrir no E10. Identificar FREEZE CANDIDATO; NÃO compilar por conta própria. Fornecer SCRIPT DE RETORNO AO CHATGPT e SCRIPT E10 sujeito à liberação.
```

---

## SCRIPT 5 — CHATGPT GPT-6 ALTO · GATE FREEZE E E10
**Finalidade:** concluir sem builds de tentativa por melhoria.

```text
DANPLUS 2.1.0 — FREEZE DE CÓDIGO E HOMOLOGAÇÃO FINAL.
Ferramenta: ChatGPT normal GPT-6 Alto para gate/arquitetura; terminal nativo + Gradle/ADB para atividades mecânicas; Codex somente para investigar erro concreto.

Antes de autorizar E10: confirmar por evidência a conclusão dos lotes E01–E09 aplicáveis, código E11 desativado, 146 IDs reconciliados, RSQ sem cópia desnecessária, contratos Scout/E06/E05 fechados, revisão do diff integral, testes escritos, assinaturas, certificados, app IDs, ABI, versionCode e rollback. Se faltar requisito aprovado, FREEZE FAIL e NÃO executar build.

Após FREEZE PASS e autorização específica: identificar tasks Gradle reais no projeto, selecionar unitários/integração que precisarão compilar e uma única rodada planejada de compilação final para as variantes Fire e Shield (APK(s) finais com assinaturas compatíveis). Não inventar nomes de task. Se FAIL, revisar erro pontual, reabrir freeze controladamente, corrigir e executar de novo o necessário, sem publicar um APK de tentativa.

Depois de build PASS: validar APKs e SHA256, signing e OTA in-place contra o 2.0.2, armazenamento de perfis, reprodução Fire/Shield, REMUX de áudio HD/HDR/DV, legendas, Scout, Calendário, Grade, backdrops, foco/DPAD, Seekr, avatares, trailers e fotos/screenshots reais nas mesmas condições. Marcar cada caso PASS/FAIL/NÃO TESTADO, anexar evidência. Publicação no GitHub, assinatura de distribuição, deploy backend e atualização das TVs exigem aprovação específica; não automatizar promoção.

Retornar FEITO / RESULTADO / PENDÊNCIAS / RESTAURAÇÃO / PRÓXIMO PASSO EXATO e scripts de continuidade. Build compilada NÃO substitui smoke visual e físico.
```

---

## Formato de retorno obrigatório (TODAS AS IAs)
```text
DANPLUS 2.1.0 · RETORNO [E__] · [DATA]
Ferramenta/modelo/raciocínio usado:
Base verificada (PC/repo/branch/HEAD/worktree):
Fontes lidas / fontes não acessadas:
Autorização disponível (planejamento / leitura / patch / build / deploy):
FEITO (ações realmente executadas):
Arquivos alterados (ou NENHUM), commit/SHA/diff:
IDs COBERTOS (já existe / parcial / ausente / investigado / excluído):
Contratos entregues e telas pertencentes a outro megalote:
Evidências e testes estáticos (PASS/FAIL/NÃO TESTADO):
Kotlin testes / compilação / APK / smoke físico / OTA (PASS/FAIL/NÃO TESTADO):
Riscos, bloqueios, segurança, rollback:
Requer decisão humana? Qual, exatamente?
SCRIPT DE RETORNO AO CHATGPT (autossuficiente):
SCRIPT DA PRÓXIMA SEQUÊNCIA (condicionado ao gate, sem avanço automático):
```

**FIM DOS SCRIPTS.** Nenhuma tarefa executada no computador ou repositório nesta entrega. Não converter scripts em ordem irrestrita de alteração: a autorização para planejar não libera patches, build, deploy ou publicação.
