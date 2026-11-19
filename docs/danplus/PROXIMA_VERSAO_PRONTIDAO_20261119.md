# DanPlus — Preparação oficial da próxima versão

**Conferência:** 19/11/2026. **Estado:** planejamento e precheck; nenhuma nova versão compilada, instalada ou publicada.

## 1. Baseline real protegida

- Novo repositório oficial: https://github.com/danilopagotto82/DanPlus
- Fonte de desenvolvimento segura: D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119, branch main; SHA-base verificado antes desta atualização documental: 9362b23b9945268ec3d3a65ebb2bf75f669e288c. Revalidar HEAD antes de implementar.
- Último aplicativo homologado: DanPlus 2.0.2, versionCode 1644. Pacote Shield com.danplus.shield; Fire com.danexus.tv.fire.debug. Builds e assinaturas originais **não devem ser alterados**.
- Release binária oficial de compatibilidade OTA: https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases/tag/2.0.2. O repositório legado deve continuar disponível.
- **Próxima versão definida pelo proprietário: 2.1.0.** versionCode deve ser maior que 1644, mas ainda não foi escolhido nem alterado no código. Consultar `PLANO_DANPLUS_2_1_0.md`. NÃO alterar versionCode ou nome da versão instalada antecipadamente.
- Fonte da APK homologada está preservada na branch legada danplus/mlc1-202-release-only-20261119, commit c92187031a204a4a01530f0b8662205c598b1e81. Os arquivos de fonte do repositório novo **não são equivalentes bit a bit** aos APKs aprovados.
- **Atualização de decisão em 19/11:** o proprietário autorizou expressamente descartar as cópias locais antigas do DanPlus, inclusive a worktree da ponte OTA com 13 entradas dirty, após conferir a publicação e preservar assinatura, APKs 2.0.2, fonte oficial e plano 2.1.0. Esta autorização não inclui outros projetos nem dados pessoais.

## 2. Pacote de mudanças: PRESENTE E CONFERIDO

- GEMINI.md e docs/danplus/gemini-planejamento/INDICE.md publicados na main oficial.
- 17 documentos de especificação/decisões confrontados com os originais em D:\APK\Organizacao DanPlus 2.0: **17/17 SHA256 iguais** em 19/11/2026. O INDICE.md é adicional.
- DP_CODEX_00…10, regras de aceitação, matriz 146 original/ARC, decisões Reshaped, 14 REQ16 e decisões MUD R3 estão registrados. Estes documentos contêm históricos de 2.0.0/2.0.1: não devem prevalecer sobre a versão instalada 2.0.2 e o Git atual.
- Melhorias importantes sem duplicar: **LOGO-PTBR-01 / REQ16-05** (logo TMDB primeiro PT-BR; só se não existir, idioma original da obra; sem preferência automática por inglês); **BUG-BACKDROP-01** (imagens de Descobrir/Indicações aparecem sem abrir Detalhes e voltar); **BUG-THUMB-01** (miniatura com quadro inferior cortado); melhorias de Seekr/DPAD, progresso, fontes, legendas/áudio/capítulos; trailers YouTube PT-BR + cache Supabase em missão independente; monitor duplo Nuvio Original + ARC apenas planejado.
- **Não interpretar PLANEJADO como CODIFICADO ou PASS.**

## 3. Bloqueios antes de qualquer novo APK

1. **Reconciliar fonte com 2.0.2 homologada.** No repositório novo a chave fixa de fallback do YouTube foi retirada, substituída por propriedade local opcional YOUTUBE_INNERTUBE_FALLBACK_KEY (local.properties/local.dev.properties), ainda sem build/teste; pode causar fallback de trailers diferente se vazia. Investigar comportamento e decidir solução segura antes do próximo release, sem copiar chaves para Git.
2. A dependência gitlink libmpv-android do repositório legado não foi importada no snapshot novo, por faltar URL .gitmodules verificada. Inspecionar o uso real no Gradle/players; as AARs precompiladas existem. Não supor que é um bloqueio de build sem teste.
3. Os sete workflows antigos permanecem desativados; nenhum release, PR automerge, cron ou build automático pode ser reativado sem revisão e gate.
4. Dois alertas Google API Key do histórico **legado** ainda estão OPEN/UNKNOWN. O repo novo não tinha alertas abertos na conferência, mas restrições/validade no titular Google Cloud não foram provadas. Não duplicar o histórico.
5. Fazer precheck de Gradle/JDK/SDK/assinatura sem revelar segredo, proteger dados/perfis e checar outros projetos/executores. Novo build da fonte sanitizada: **NÃO TESTADO**.
6. O crash Compose na tela Detalhes foi relatado na 2.0.0. Antes de priorizar patch DP-ML01, verificar se ainda ocorre na 2.0.2; causa histórica não comprovada. Sem reprodução/evidência, não inventar conserto.

## 4. Execução futura em megalotes

- **DP-ML00 — primeiro:** somente leitura, identificar diferenças de fonte, dependências/build, riscos atuais, histórico de erros, código de Focus Dock, cache de imagens, proteção de OTA e estado de Git. Relatório com PASS/FAIL/NÃO TESTADO e arquivos exatos. Não criar APK nessa etapa.
- **DP-ML01 — condicional:** estabilidade Detalhes/Compose, somente se ainda houver defeito demonstrável. Worktree e branch novas isoladas; patch mínimo e regressão.
- **DP-ML02 — UX de alto valor:** foco/DPAD, prioridade de logos PT-BR, backdrop carregando no foco, miniatura cortada. Reusar componentes existentes, não reimplementar MUD, confirmar screenshots Fire e Shield.
- **DP-ML03…08:** históricos/calendário; failover fontes/player; capítulos/legendas/áudio; validação UX existente; backend/RLS com aprovação de produção separada; trailers/caches como missão própria.
- **DP-ML09 já concluído em 2.0.2:** preservar OTA e nome DanPlus. Próximas versões devem atender ambos os repositórios até comprovar que não há instalações antigas dependentes do legado.
- **DP-ML10:** regressão e homologação física de ambos os aparelhos antes de autorizar nova distribuição.
- **DP-ML11 opcional:** monitorar Original + ARC via GitHub, sem Codex recorrente e sem implantação automática de APK.

## 5. Ferramentas e etapas para a próxima missão

- **GPT-6 Alto no ChatGPT normal:** coordenar, analisar divergências e revisar gates.
- **Commander + Git + terminal:** precheck, inventário, testes focados e builds mecânicos sem gastar agente.
- **Codex Alto:** apenas quando houver correção real no Kotlin/Compose/player/segurança; usar worktree nova a partir do HEAD oficial conferido. Não programar em main nem na worktree dirty protegida.
- Fluxo: **PRECHECK → PLANO → PATCH → TESTE FOCADO → BUILD → SMOKE FIRE/SHIELD → REVISÃO DO DIFF → PASS/FAIL**. Um build pesado de cada vez. Sem teste físico, não marcar smoke PASS.
- Proibidos sem gate: limpar projetos/worktrees, apagar assinaturas/backups, modificar production Supabase, mudar applicationId/certificados, instalar em TVs, publicar release ou reativar workflows.

## 6. Próxima ação exata

Abrir este documento, GEMINI.md e o INDICE de megalotes; confirmar o HEAD e a main limpa do repositório novo. Fazer **DP-ML00 em somente leitura** e solucionar primeiro o baseline de compilação seguro (fallback YouTube e módulo libmpv), identificando se o crash de Detalhes permanece na 2.0.2. Só então escolher o primeiro patch da próxima versão.

**Limpeza do PC:** ocorre em trilha separada, com inventário e aprovação dos caminhos exatos. Não apagar builds homologados, worktrees dirty, chaves/certificados/keystore, backups, fotos nem outros projetos.
