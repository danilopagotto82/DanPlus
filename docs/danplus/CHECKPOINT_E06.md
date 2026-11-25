# Checkpoint E06 — DanPlus 2.1.0

25/11/2026 · Candidato local · Revisão estática · Homologação reservada ao E10.

## Base

- Branch `feat/danplus-2.1.0-integracao-20261125`, worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, HEAD inicial local e remoto `6f8c19f4b1da856b878f40699fc15198d33482b3`.
- Main local `e67459cfb1042fe2e68f97b0a017f1411512bffe`, main remota `04e5b4e1780c43f6de4e5627453985821c07d48b`. Ambas preservadas.
- AGENTS.md, checkpoints E02/E04/E03/E03B, plano e linhas pertinentes da matriz 146 e evidências 166 foram consultados. [Plano complementar PR #2](https://github.com/danilopagotto82/DanPlus/pull/2), documentação em `5ede65ac39c23a4b36f8a98458860cc6b1713506`.

## Gaps comprovados e correções

| IDs | Evidência e correção |
| --- | --- |
| B1-04, A1-08, A1-27, B4-D07 | A publicação no launcher lia os dois snapshots pelo perfil ativo em instantes diferentes. A reconciliação captura o perfil, lê ambos explicitamente nesse perfil e revalida antes de publicar. O agendamento periódico de contingência, coalescência e espera de saída do player do E04 foram mantidos. Identidade dos cards e recuperação de foco preexistentes. |
| A2-16, RSQ-04 | Percentuais não finitos podiam prevalecer sobre a posição válida ou representar conclusão com infinito. A projeção do progresso em `WatchProgress` usa a posição real nesses casos. Chaves por episódio, mapeamento Trakt, metadados e ordem preferida foram preservados. |
| B1-18, A1-22, A2-38, B4-D05 | A conclusão era marcada como enviada antes da resposta e o scrobble assíncrono podia entrar no provedor já com outro perfil ativo. Eventos do player carregam o perfil. Os provedores revalidam o escopo no ponto de escrita. O gate evita dois stops simultâneos e, em nova tentativa da mesma sessão, exclui provedores que já processaram o stop. O Trakt reporta falha real ao coordenador e mantém a deduplicação de stops aceitos sem apagar a transição 79,x% → 80%. A persistência local por perfil, a política de cada provedor e cadência de backups seguem os fluxos existentes. |
| B1-24, A1-09, A2-48 | Lotes de assistidos podiam enviar a mesma chave mais de uma vez e o painel exibia sucesso após falha. O repositório remove duplicatas no lote. Os comandos da tela de detalhes impedem lotes sobrepostos, revalidam perfil/conteúdo e usam feedback de erro quando a escrita falha. Confirmação e escopo da seleção existentes. |
| A1-31, A2-44, RSQ-06, B3-D03 | As duas ordenações dos próximos episódios comparavam só o dia. Agora comparam o dia local e, dentro dele, o instante real, seguido de entradas sem hora. Data ausente fica no fim. O calendário existente oferece filmes/séries, cinema, digital e disco quando informados pela fonte. Dia/Semana/Mês continuam no contrato existente. |
| B2-05, RSQ-07, B3-D07 | As notas públicas de episódios e a sincronização pessoal de títulos já existiam. O novo `TraktPersonalRatingRepository` oferece leitura, escrita e remoção da nota pessoal de longa ou episódio por perfil. Episódios exigem IDs próprios. A escrita serializada consulta o valor atual para evitar duplicação e confere o recibo e `not_found`. Nenhuma nova interface ou prompt. [Contrato de escrita Trakt](https://raw.githubusercontent.com/trakt/trakt-api/master/projects/api/src/contracts/sync/schema/request/ratingsParamSchema.ts) · [Recibo Trakt](https://raw.githubusercontent.com/trakt/trakt-api/master/projects/api/src/contracts/sync/schema/response/ratingsResponseSchema.ts) |
| B1-06, A2-40, A2-43, A2-46 | PostPlay filtrava duplicatas só por tipo e ID bruto. Agora reconhece aliases de IMDb e série/TV, sem fundir títulos só pelo nome ou números iguais de tipos diferentes. Listas de premiações já deduplicam IDs da fonte. Próximo episódio e filtros familiares existentes preservados. Estados vazios e apresentação permanecem no E05. |

## Preservação e pontos de investigação

- **B1-03** investigação. A persistência, observadores e escopo por perfil já existem. A atualização instantânea de assistido individual ao navegar entre addons permanece para smoke.
- **B1-11/B4-D04** investigação. A leitura estática encontrou os argumentos iniciais no `SavedStateHandle` e os campos em memória atualizados na troca de episódio. Não foi localizada uma persistência equivalente da rota atual. A correção de replay do intent/rota tem gate de reprodução após morte do processo. Preservado o caminho atual, inclusive fontes temporárias e proteção de credenciais.
- **B1-15, B1-26, B2-04, A2-28**. O seletor de listas já consome associação por lista. Nome da série, busca de fonte do episódio e cancelamento ao fechar a busca foram mantidos. **B3-D06** preserva os metadados de temporada/especiais; cartaz e foco visual seguem para E05.
- **RSQ-05** preserva o adiamento E04 de prefetch e sincronização em playback. **B4-D06/RSQ-10** permanecem LAB. Rewatch Simkl, ordenação TVDB alternativa, novos feeds familiares/esportivos e integrações pagas ficam pendentes de contrato e autorização próprios.
- **E03B** mantém a identidade do longa. Eventos do extra continuam fora de progresso, assistido, scrobble e PostPlay. Uma gravação legítima do longa já capturada é mantida no perfil original, com guarda adicional antes de publicar no Watch Next. O leitor Blu-ray ficou intacto.
- **E05/E07B/E08**. E05 pode consumir o novo contrato de notas e os contratos existentes de calendário e foco. Descubra-me permanece no E07B. Autenticação e integrações novas do WeTrakr permanecem no E08. Os ajustes em tracking atuam apenas nas integrações presentes.

## Regressões e gates

- Onze casos novos escritos para fuso, hora desconhecida, NaN/infinito, STOP Trakt na transição de conclusão, retries sem reenviar ao provedor aceito, perda temporária de conexão, tentativa obsoleta, notas com identificação errada, aliases PostPlay e lote deduplicado.
- **E10 — Retomada.** Em série, trocar do episódio 3 para 4, deixar tocar, enviar o Android ao segundo plano, matar o processo e comparar reabertura por Recentes, launcher e Continue Assistindo. Repetir em troca de temporada, especial, episódio fracionado e troca de perfil. Verificar foco, posição, IDs e fontes expiradas antes de mexer no contrato B1-11.
- **E10 — Histórico.** Alternar perfis durante leitura, escrita e resposta tardia. Forçar perda de rede antes e depois do STOP, comparar progresso local/externo, repetir marcação e desmarcação em lote e verificar o histórico original. Validar recibos reais, limiares dos provedores e ausência de backup extra. Rewatch Simkl segue LAB.
- **E10 — Calendário e personalização.** Validar dias com fuso diferente, entradas sem hora/data, especiais, filmes no cinema/digital/disco e filtros existentes. Testar notas pessoais Trakt de longa e episódio e listas com aliases/duplicatas. Conferir o fluxo do launcher no hardware suportado.
- **E10 — Regressão cumulativa.** Reproduzir MKV/REMUX, HLS/DASH, fallback de fonte E02/R1, seek/cache E04, AFR E03/R1 e retorno de extra E03B após fim/erro/saída. Verificar que o extra e callbacks tardios não mudam dados do longa.

O retry de conclusão no E06 é restrito à sessão existente. Reexecução após morte de processo, respostas ambíguas do provedor, diferenças de catálogo e troca de conta exigem validação no E10. O contrato de nota pessoal foi conferido no esquema Trakt; integração com conta real permanece pendente.

- **PASS estático** revisão do diff, `git diff --check` e análise sintática dos 31 arquivos Kotlin.
- **FAIL** nenhum nas verificações finais executadas.
- **NÃO TESTADO** execução dos casos JUnit, Gradle, build, instalação, smoke, comportamento de processo Android, rede de tracking com conta real e launcher no aparelho.

Sem Gradle, build, APK, instalação, push, merge, release, alteração de main, aplicação em produção, mudança de perfil homologado, assinatura, IDs, OTA, MPV ou descarte remoto Silo.

Próxima ação: auditar o único commit local acima de `6f8c19f` via Commander e decidir se libera o push. Homologação e reprodução B1-11 permanecem no E10.

## E06-R1 — Confirmação de scrobble

25/11/2026 · Candidato local para auditoria independente · B1-18, A1-22, A2-38 e B4-D05.

- Base local `c0638db0c8be7b0d53e96533bec77e549d80113f` na mesma branch e worktree. Na consulta remota, a branch de integração estava em `6f8c19f4b1da856b878f40699fc15198d33482b3` e `main` em `04e5b4e1780c43f6de4e5627453985821c07d48b`. A ancestralidade contém E02, E02-R1, E04, E03, E03-R1, E03B e E06.
- O contrato dos cinco provedores distingue confirmação, duplicação comprovada, operação ignorada, indisponibilidade, falha recuperável e mudança de perfil ou conta. Retornos antecipados e recibos de progresso continuam pendentes para conclusão. No Trakt, `409` do STOP terminal é a duplicação documentada e a conclusão 2xx exige ação `scrobble`. No Simkl, `409` do STOP exige `watched_at` válido. No MDBList, progresso alto sem ação de conclusão ou data de assistido não cria histórico. No WeTrakr, STOP terminal exige `201` e mantém o limiar de 90%. Floppy reaproveita o identificador de conclusão no retry. [API Trakt](https://trakt.docs.apiary.io/reference/sync) · [API Simkl](https://simkl.docs.apiary.io/reference/redirect)
- Pausa capaz de concluir e STOP passam pelo mesmo gate. STOP concorrente com pausa é adiado uma vez antes de reenviar apenas aos pendentes. A troca de episódio reinicia o gate nas rotas direta e de recriação do player. O retry fica na mesma sessão, preserva delegação ao servidor e não usa as credenciais de outra conta. O refresh Trakt também revalida a autorização original antes de gravar ou limpar credenciais.
- E03B mantém posição e restauração do longa. Extra ativo bloqueia scrobble, e callbacks de preparação e conclusão revalidam o item e a transição de extras. Os leitores Blu-ray, PostPlay e a persistência de progresso não foram modificados.
- Onze casos novos e atualizações em testes existentes cobrem retorno sem envio, identidade ausente, recibo insuficiente, desconexão/reconexão, retry parcial, duplicata confirmada, troca de perfil/conta durante envio ou refresh, STOP concorrente, resposta tardia, ID de retry e isolamento de extras.
- **PASS estático** revisão do diff, `git diff --check`, ancestralidade Git e análise sintática PSI Kotlin `files=29 errors=0`. **FAIL** nenhum nas verificações finais executadas. **NÃO TESTADO** resolução de tipos por build, JUnit, serviços com contas reais, dispositivo, smoke e recuperação após morte do processo.
- **Limites** o retry depende de novo evento válido na mesma sessão. Uma resposta perdida após o envio pode exigir nova tentativa, e a deduplicação depende do contrato de cada provedor. Validar recibos reais no E10, em especial o STOP de conclusão e a desconexão após a escrita remota.
- **B1-11/B4-D04** segue no E10 com o roteiro episódio 3 → 4 → segundo plano → morte do processo → retomada por Recentes, launcher e Continuar Assistindo. A rota atual segue preservada.

Sem Gradle, build, instalação, push, merge ou release. Main, produção, assinatura, OTA e configuração de perfis preservadas. E07B segue fora deste lote.

Próxima ação — o coordenador deve comparar o commit local E06-R1 com `c0638db` via Commander, conferir os recibos e a serialização de PAUSE/STOP e liberar explicitamente o push antes de publicar.
