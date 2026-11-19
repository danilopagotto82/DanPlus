# DANPLUS · DP-CODEX-08 — ESTUDO PROFUNDO DE AUTOMAÇÃO UPSTREAM
Data: 17/11/2026. **DOCUMENTO DE ARQUITETURA; NENHUMA AUTOMAÇÃO CRIADA/EXECUTADA.** Não inclui autorização de programação, instalação ou publicação.

## Problema que queremos resolver
O proprietário não deseja contratar uma nova auditoria manual/IA a cada lançamento do Nuvio. Objetivo: detectar releases novas, comparar com o DanPlus e as decisões já registradas, produzir status técnico fácil de ler e preparar integração isolada somente quando autorizada, economizando tempo/consumo de IA. Não permitir regressões automáticas, perda de funcionalidades ou publicação prematura.

## 1. Auditoria do fork real — NuvioTV-Reshaped
Origem: https://github.com/DavidVamaiotu/NuvioTV-Reshaped
- Workflow investigado (branch `subtitle-autosync`): `.github/workflows/mirror-nuviotv-release.yml`, URL https://github.com/DavidVamaiotu/NuvioTV-Reshaped/blob/subtitle-autosync/.github/workflows/mirror-nuviotv-release.yml
- Trigger configurado: `workflow_dispatch` (manual), push excepcional e `schedule: 17 */2 * * *` (consulta a cada duas horas se estiver operante).
- A cada agendamento: `gh api repos/NuvioMedia/NuvioTV/releases`, seleciona última release não-draft (beta incluída), captura tag e prerelease, detecta se há commit anterior `Sync NuvioTV <tag>`.
- Se houver release nova: busca a tag upstream no Git, faz `git merge --no-ff --no-commit` no fork e preserva `.github/workflows` existente. Em conflito inesperado, INTERROMPE. Conflito especial conhecido em `app/build.gradle.kts` toma o upstream e reinsere o hook `reshaped.gradle`. **Isso não prova preservação de todas as personalizações em toda mudança futura**.
- Execução: `python scripts/test_seek_preview_native.py`, `./gradlew :app:testFullReleaseUnitTest`, build APKs por `.github/scripts/reshaped-tv-apks.sh`; verifica package, SHA256 do certificado e versionCode > anterior na variante arm64; gera release em GitHub quando PASS. Verificador de compatibilidade não substitui instalação real nem testes de usabilidade.
- O workflow `autosync-debug.yml` é CI de compile em push e não é o detector de release oficial.
- Evidência GitHub Actions API: em 17/11, builds AutoSync Release em `workflow_dispatch`; as últimas execuções `schedule` AutoSync TV Release encontradas em 09/10, com sucessos e falhas anteriores. Repositório atualmente declara `default_branch=animeIntegration`, enquanto o arquivo monitor existe em `subtitle-autosync`. A documentação GitHub exige workflow de schedule na branch default. **Logo, a configuração de duas horas pode NÃO funcionar no estado atual.** Não alegar monitoramento automático ativo somente por ler o YAML.
- API Actions: https://api.github.com/repos/DavidVamaiotu/NuvioTV-Reshaped/actions/runs?event=schedule&per_page=100; últimas schedule AutoSync em 2026-10-09; releases recentes (2026-11-17) publicadas por github-actions[bot], mas GitHub Actions foi disparado MANUALMENTE. "Publicado por bot" NÃO é prova de gatilho automático.
- Branch padrão atual e política do agendador devem ser revalidadas antes de copiar o workflow. GitHub Actions pode atrasar ou perder scheduled runs; em repositório público, workflow programado pode ser desabilitado após 60 dias de inatividade (docs oficiais).
- Referências normativas: https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows ; https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target

## 2. Veredito
- **VIÁVEL** automatizar descoberta upstream, diferenças de commits, classificação prévia por pasta/arquivos, relatório de impacto e abertura controlada de issue/PR com revisão.
- **VIÁVEL MAS NÃO GARANTIDO** automatizar merge experimental e compilar em worktree/runner descartável com testes sintéticos.
- **INSEGURO** atualizar automaticamente os APKs de produção ou publicar OTA após um merge apenas por compilar, porque DanPlus tem dois packages/assinaturas, diferenças de Fire 32-bit/Shield 64-bit, MPV/ExoPlayer, MUDs próprios, Supabase, Seekr e P0 de Detalhes ainda não corrigido.
- **NÃO PROMETER** que a auditoria automática determina com certeza equivalência funcional/UI/codec ou que uma nova versão upstream roda sem falhas em ambos aparelhos.

## 3. Desenho recomendado — ETAPAS INDEPENDENTES E REAPROVEITÁVEIS
**DA-00 (primeiro, barato, somente leitura):** GitHub Actions programado 1–4 vezes ao dia (ou outro agendamento aprovado) no branch DEFAULT do repositório autorizado, manual_dispatch adicional. Consultar OBRIGATORIAMENTE releases de Nuvio Original (NuvioMedia/NuvioTV) e Nuvio ARC (ysosrs123/NuvioTV-Fork) com marcadores independentes; Reshaped é fonte complementar opcional. Regras completas em DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md. Usar ETag/If-None-Match ou SHA/tag persistido; não gerar alertas por repetição. Sem credenciais de player/API e com `contents: read` se possível. Guardar últimos tags/SHAs e estado do monitor fora do app/OTA. Monitorar também interrupção do próprio workflow.
**DA-01 (auditoria mecânica):** para nova tag, comparar commit/tag base anterior com novo: diff de arquivos, changelog, lista de commits, categorias (UI, DPAD/Compose, player, Seekr, legendas, memória, Trakt, banco/API, Gradle, dependências, segurança). Gerar relatório em PT-BR com links reais, SHA upstream, data, diffs e testes NÃO TESTADOS. Conciliar com 146 IDs e 7 RS mais MUD/REQ16 e bugs abertos; **não alterar automaticamente o status humano**, não incluir itens já codificados como tarefa nova. Determinar REDUNDANTE, CANDIDATO ADAPTAR, REQUER DECISÃO, RISCO/HOLD, sem alegar validação funcional.
**DA-02 (opcional, depois de aceitar DA-01):** gerar PR de análise/issue no repositório adequado, sem tocar main/prod. Para patches previamente aceitos e com fronteira bem definida, abrir PR DRAFT em branch isolada ou job temporário com proposta; no caso de conflito suspender e produzir diff e alerta. NUNCA forçar merge. Revisar submódulos/dependências e obrigações GPL.
**DA-03 (somente quando houver patch candidato validado):** CI automático sem segredos de distribuição: testes unitários, lint, análise estática, ABI e build debug/test; reportar resultado. Rodar testes de segurança e integração sintética. Separar `pull_request` untrusted de signing secrets; evitar `pull_request_target` rodando código não confiável com token de escrita. Não misturar com migração/produção.
**DA-04 (HOLD até gate humano):** revisão manual de conflitos de produto, smoke físico Shield/Fire, screenshots contra design aprovado, áudio lossless, REMUX e reprodução; assinatura/OTA com certificados originais autorizados; release e instalação só mediante gate próprio. Bot nunca publica build de produção diretamente do monitor.

## 4. Regras automáticas de triagem
- **Baixo risco / leitura:** textos PT-BR novos, docs, imagens externas, pequenas correções locais — gerar recomendação e arquivos alterados, não copiar automaticamente sem decisão.
- **Moderado:** Home/foco, LazyList, dados do Trakt, legendas, fontes, mudanças no player — CI focado + inspeção humana nos casos que tocam código próprio.
- **Alto risco / bloqueio:** applicationId, versionCode, keystore, Gradle, updater/OTA, RLS/SQL/remote auth, rede de fontes, segurança/privacidade, codecs/FFmpeg/libmpv/ExoPlayer, memória 32-bit, cron/Actions, termos e licenças. Nunca auto-merge/publicar.
- **Ignorar como patch e manter registro:** B2-06 e RS-06 PENDENTES; A2-35 NÃO RECOMENDAR; qualquer decisão INVESTIGAR sem prova de bug; itens MANTER; patches duplicados de MUDs já existentes.
- Alertas devem mostrar `novo / já presente / incompatível / não verificado / decisão humana pendente`, com **confiança baseada em evidências, nunca porcentagem inventada**.

## 5. Adoção no projeto real e riscos de infraestrutura
- Repositório antigo atual: danilopagotto82/DANPLUS-Shield-Arc31-Private, com main e rota OTA protegidos. **Não mudar default branch, README remoto, GitHub URL nem criar Actions agora sem gate**. O novo repositório planejado danilopagotto82/DanPlus ainda não foi criado nesta missão.
- Colocar o monitor em branch non-default impede schedule tradicional. Opções futuras: (a) workflow READ-ONLY no branch default do repo atual sem mexer no app/OTA, após aprovação e revisão; (b) repositório de automação separado, sem releases OTA nem secrets; (c) esperar o futuro repo DanPlus receber branch main configurado, mantendo o repo legado.
- Se correr em GitHub público, evitar logging de credenciais, GITHUB_TOKEN com `contents:write` em job que executa código upstream, paths com secrets e `pull_request_target` inseguro. PRs gerados com token separado e permissões mínimas; jobs sem capacidade de publicar.
- Prevenir filas de builds e consumo gratuito: detectar só tags inéditas, pular builds enquanto não há patch candidato; CI barato sempre, build/Gradle só depois de triagem. GitHub Actions pode ter limites/custo conforme plano e política; não inventar números.
- DaNEXUS, DAN TV+, ÓRBITA, MasterFinance e ponte OTA ML-C1 ficam completamente fora do escopo.

## 6. Aceite futuro, sem autorizar implementação nesta pesquisa
- Duas execuções sem release nova => zero PRs e zero builds pesados.
- Uma release nova => relatório único com tag, SHA, changelog, full diff, categoria/risco, impacto DanPlus, itens conflitantes, status pendente e links; alerta, sem tocar no app.
- Release repetida, republicação da tag ou ação concorrente => deduplicar por tag+SHA e travar com alerta se mesma tag mudar de commit.
- Branch default e cron executando de verdade: evidência de workflow scheduled e saída esperada, incluindo alerta para ausência de execução. Smoke do workflow/manual dispatch não é prova de cron.
- Merge conflituoso ou dependência incompleta => FAIL/HOLD, preservar fonte, relatório de conflito; NUNCA fazer push/merge.
- Apenas após QA físico e autorização explícita: etapa de release distinta, com assinaturas, pacotes e versionCodes corretos e plano de reversão.
- Os dois aparelhos serão usados para smoke **apenas** com autorização, nunca por agendamento silencioso.

## 7. Situação em 17/11/2026
- Investigação GitHub, leitura YAML/script e workflow runs: **PASS em somente leitura**.
- DA-00..DA-04: **NÃO IMPLEMENTADOS**; nenhum novo workflow, PR, repo, tag, issue, instalação, APK ou merge criado.
- Recomendação de implementação: nova missão futura **DP-ML11 — Monitor automático upstream, primeiro só leitura**, independente de DP-ML01 e DP-ML09. Raciocínio adequado: ChatGPT normal GPT-6 Alto para desenho/revisão; Codex Alto somente para desenvolver workflows/scripts em branch isolada; GitHub Actions como executor de baixo consumo sem IA em cada execução.
- Objetivo econômico: reduzir auditoria humana recorrente, não burlar revisão do produto. Alertar proprietário só sobre novidades relevantes ou conflitos, sem prometer que nenhuma intervenção futura será necessária.
