# DanPlus · Estudo: automação de upstream e revisão no GitHub, sem Codex recorrente
Data: 18/11/2026. **SOMENTE ESTUDO E PROJETO; NÃO IMPLEMENTADO.**

## Achados verificados
- Github conectado: Nuvio Original `NuvioMedia/NuvioTV` (público, branch default `dev`); Nuvio ARC `ysosrs123/NuvioTV-Fork` (público, default `main`).
- Repositório legado `danilopagotto82/DANPLUS-Shield-Arc31-Private` aparece como PUBLIC no GitHub, **visibilidade expressamente desejada pelo proprietário em 18/11/2026; NÃO sugerir privatização**. Novo repo planejado `danilopagotto82/DanPlus` não existe/retornou 404 neste teste. Não criar, renomear ou mudar permissões sem gate próprio.
- Workflows reais Nuvio original no branch dev:
  - `.github/workflows/pr-template-check.yml`: exige descrição, critérios e evidência em PR.
  - `.github/workflows/pr-full-debug-build.yml`: testes do updater + build APK debug em PR para dev; separa signing de CI de chave release.
  - `.github/workflows/pr-full-debug-build-comment.yml`: comenta automaticamente resultado e artefato no PR.
  - O Nuvio também tem execuções registradas de Copilot code review; isto não é um fluxo gratuito garantido ao DanPlus.
- GitHub Actions padrão em repositório público: runners padrão grátis segundo documentação GitHub; privado depende de franquia/billing. Schedule tradicional apenas no branch DEFAULT; pode atrasar e auto-desligar em repo público por 60 dias sem atividade.
- CodeQL/dependency review gratuitas em repositórios públicos (aplicabilidade Java/Kotlin/Gradle deve ser confirmada nos arquivos do DanPlus). Segurança estática não comprova UX/codec/assinaturas.
- CodeRabbit em 18/11 anuncia revisões de PR com IA gratuitas para **repositórios públicos**, sem cartão; para privados não presumir grátis.
- Gemini Code Assist consumidor GitHub foi DESCONTINUADO em 17/07/2026. Enterprise/Google Cloud disponível com projeto e conta de faturamento, não equiparar ao Google AI Plus. Google `run-gemini-cli` é uma Action separada, requer chave API e controle de consumo; não confundir.
- Copilot code review possui consumo de AI credits e possivelmente minutos de Actions; NÃO ligar automaticamente por padrão.
- Revisor por IA em PR analisa ALTERAÇÕES PROPOSTAS, não descobre sozinho novidades externas, nem aplica automaticamente release Original/ARC ao DanPlus.

## Arquitetura preferida: GitHub Watchtower DanPlus (conceito)
**CAMADA 1 - WATCH (sem IA, automático):** workflow leve no GitHub do MONITOR, não no legado/OTA. Schedule sugerido 2 vezes/dia + execução manual. Acessar APIs das releases **de ambos** Original e ARC; opcionalmente PRs merged desde checkpoint, com limite e paginação. Registrar por repo, ID de release, tag, SHA e URL. Evitar 'latest-only'; processar múltiplos releases entre verificações, identificar mutação de tags, lidar com rate limit e falha parcial. Persistir ledger durável (ex.: issue de controle no repo do monitor, ou commit de estado sob permissão isolada) evitando perda de checkpoints com artifacts temporários. Backfill inicial sem alertar como se tudo fosse novo.
**CAMADA 2 - DIFF (sem IA):** comparar cada fonte com ela mesma (versão anterior vs nova), correlacionar Orig→ARC com evidências; classificar arquivos alterados, temas, prioridades e riscos. Conferir catálogo 146 IDs/36 decididos, RS 7/6 decididos, MUD R3 14, B2-06/RS-06 HOLD, P0 Detalhes e bug miniatura. Se base local DanPlus não estiver disponível no monitor, marcar COMPARAÇÃO COM DANPLUS NÃO VERIFICADA; nunca carregar worktree privada, .wrangler, keystore ou secrets em runner público. Code hashes/relatório de arquivo não provam recurso equivalente.
**CAMADA 3 - TRIAGEM PROFUNDA (IA apenas por evento novo relevante, OPCIONAL):** via provedor aprovado, chamar uma única análise sem código privado em fonte pública; cobrar/quota somente se aprovada. Estruturar resumo PT-BR com o que mudou, evidência, classe, duplicidade provável e recomendação. Para checar compatibilidade com código DanPlus real, executar auditoria em ambiente privado/local sob aprovação, ou contra versão de código intencionalmente publicada e revisada. Nunca presumir acesso.
**CAMADA 4 - REVISÃO DE PR (automático quando existe PR):** GitHub Actions para testes lint/Kotlin/Gradle e relatórios; em repo público, CodeRabbit pode revisar PR automaticamente. Essa análise NÃO é monitor de releases. Em repo privado avaliar custo de Gemini CLI com chave/limite ou modelo local no PC. Revisão não equivale a QA físico. PR DRAFT somente após aprovação de propor um patch.
**CAMADA 5 - PRODUÇÃO (HOLD HUMANO):** nada de merge, release, tags, assinatura, instalação, Supabase remoto, APK OTA ou mudança dos aparelhos. Gate de QA Shield/Fire e comparação visual, trilha OTA 2.0.1 independente.

## Fluxo previsto de cada dia
1. A cada execução, checar Original + ARC e status do próprio agendador.
2. Sem novidades: registrar heartbeat mínimo; nenhum PR/build ou custo de IA.
3. Com novidades: emitir um relatório deduplicado, links, tags, SHAs, risco e itens relacionados.
4. Se apenas informação: manter Issue/relatório atual sem duplicar alertas.
5. Se proposta de patch: gerar checklist de decisão humana; após aceite, abrir trabalho separado. Nunca abrir automaticamente PR de código não inspecionado no app.
6. Ao abrir PR aprovado: lint/tests + revisor IA opcional; se PASS, ainda requer QA físico e gate final antes de publicar.

## Comparativo econômico
- GitHub Actions + scripts sem IA: primeira opção para monitor/diff; gratuito em runners padrão para repo público, com políticas de uso e segurança a verificar.
- CodeRabbit: revisão AI gratuita em repo público, sem equivalente gratuito confirmado para privado.
- GitHub Copilot: recurso real, mas depende de licença/AI credits; NÃO associar a ChatGPT Plus.
- Gemini CLI via GitHub: usa API/chave, tem free tiers dependendo do serviço e quota real, que precisa verificar ANTES de habilitar; a assinatura Google AI Plus não substitui API.
- Codex: NÃO NECESSÁRIO para executar o monitor diariamente; um setup inicial simples pode ser feito com ChatGPT + Commander/Node/GitHub Actions e testes. Codex continua opcional para patches Android difíceis.
- CI Gradle completo: só ao existir patch autorizado, não a cada polling.

## Security & precheck
- **A visibilidade PÚBLICA foi aprovada e deve ser preservada.** Inspecionar apenas exposição acidental de credenciais/segredos, sem exibi-los em logs; nome do repo não define visibilidade. Não privatizar, renomear ou alterar histórico/repositório legado por iniciativa própria.
- Definir onde criar repo do monitor, visibilidade pública/privada, permissões, custo, credenciais e email/issue de notificação; NÃO criar agora.
- Não usar `pull_request_target` para executar código externo com token de escrita; monitor simples sem checkout de upstream executável.
- Evitar instalar GitHub Apps com permissão 'all repositories'; autorizar repositório específico somente com confirmação do dono.
- Não usar token/chave de release, assinatura, Supabase, OTA ou aplicativo; logs devem mascarar credenciais.
- Regras de retenção: issues/ledger persistente, dedup, reconciliação ao falhar API, alerta watchdog para cron inativo.
- Não chamar 'análise automática completa do código DanPlus' sem ter snapshot seguro do código baseline a ser analisado. Não confundir CodeQL/CodeRabbit/IA com teste de hardware.

## Evidências de teste esperadas para aceitar futuro monitor
- Sem novidade em duas fontes -> zero relatórios duplicados; Original-only; ARC-only; ambas com mesma feature; duas releases entre execuções; tag mutada SHA divergente; falha 403/429 só em uma fonte; concorrência; timeout; heartbeat ausente; novo issue por evento único; reexecução idempotente; nenhuma alteração app/main/OTA.
- Executar workflow via manual + schedule real, confirmar outputs com release sandbox/fixture e corrigir desvios; registrar PASS/FAIL/NÃO TESTADO. Teste de schedule não comprovado apenas por workflow_dispatch.

## Referências verificadas em 18/11
- https://github.com/NuvioMedia/NuvioTV/blob/dev/.github/workflows/pr-template-check.yml
- https://github.com/NuvioMedia/NuvioTV/blob/dev/.github/workflows/pr-full-debug-build.yml
- https://github.com/NuvioMedia/NuvioTV/blob/dev/.github/workflows/pr-full-debug-build-comment.yml
- https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows
- https://docs.github.com/en/actions/concepts/billing-and-usage
- https://docs.github.com/en/copilot/concepts/agent-capabilities/agent-powered-workflows-and-operations/code-review
- https://www.coderabbit.ai/oss
- https://developers.google.com/gemini-code-assist/docs/deprecations/consumer-code-review
- https://github.com/google-github-actions/run-gemini-cli
- https://docs.cloud.google.com/gemini/docs/code-review/set-up-code-assist-github

STATUS: pesquisa e arquitetura prontas; configuração, código, workflows, PRs, monitor ativo, testes de execução e alertas: **NÃO EXECUTADOS / NÃO TESTADOS**.
Próximo passo sugerido: **visibilidade PÚBLICA já definida pelo proprietário**. Falta confirmar onde o monitor residirá e autorização específica para criar repositório/workflow novo; não alterar legado nem criar repo DanPlus por impulso. Depois ChatGPT+Commander pode implementar DA-00 (read-only) sem Codex, com teste em sandbox isolado.
