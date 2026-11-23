# DANPLUS 2.1.0 — CONTINUIDADE E ORGANIZAÇÃO DAS MELHORIAS

**Data:** 22/11/2026  
**Natureza:** adendo de planejamento, não implementação.  
**Origem:** decisões recuperadas da conversa *Pesquisar atores em cena* e conversas correlatas de planejamento, confrontadas com a Biblioteca e com `GEMINI.md` e `docs/danplus/PLANO_DANPLUS_2_1_0.md` consultados no GitHub. Não se dispõe da transcrição integral de todos os turnos.  
**Estado:** documento local preparado; **não publicado no GitHub**, pois a integração retornou **HTTP 403** ao tentar criar uma branch documental. Nenhuma escrita de código, main, APK, banco de dados ou OTA foi realizada.

## 1. BASELINE E PROTEÇÕES

- **Produto:** DanPlus; próxima versão planejada **2.1.0**, partindo dos APKs **2.0.2/1644** homologados.
- **Repositório oficial:** `https://github.com/danilopagotto82/DanPlus`.
- **Código de trabalho a conferir em E00:** `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`.
- **APKs protegidos:** `D:\APK\DanPlus_2_0_2_HOMOLOGADO`.
- **Instruções:** ler `D:\APK\AGENTS.md`, `GEMINI.md`, `docs/danplus/PLANO_DANPLUS_2_1_0.md`, matriz e roteiro de execução. Não usar caminhos de worktrees antigas eliminadas.
- **Contabilidade:** preservar intactas as **146 decisões históricas** (105 APROVAR, 5 NEGAR, 23 ADAPTAR, 8 INVESTIGAR, 4 MANTER, 1 NÃO RECOMENDAR). Os requisitos abaixo são *adendos vinculados*, não renumeram ou duplicam os 146 registros; os quatro pedidos posteriores já documentados também continuam separados.
- **Restrições:** nenhuma compilação de APK antes de concluir o código do marco; não criar um APK por melhoria; não alterar/recriar o Seekr, Scout, Calendário ou player; não editar produção Supabase/credenciais, applicationId, signing, OTA ou releases sem gate próprio. Somente Codex escreve Kotlin/Compose na fase autorizada.

## 2. DECISÃO CENTRAL — `DP21-EMCENA-01` (ATORES EM CENA)

### 2.1 Objetivo aprovado

Oferecer no DanPlus uma função **Em Cena**: no ponto escolhido de um filme ou episódio, informar **somente os atores e personagens que estão efetivamente naquela cena**, inspirado na experiência de informações contextuais de players comerciais. Usar a **miniatura Seekr existente**, sem alterar seu gerador nem pressupor que TMDB/IMDb ofereçam timecodes por ator.

### 2.2 Estratégia obrigatória — confiabilidade primeiro

**Fase A — infraestrutura de piloto, sem IA facial no APK**
1. Após E00, verificar o Seekr real e seu modelo de `timestamp`, frames/miniaturas, estado de pausa/seek e estabilidade Fire/Shield. Preservar RS-04 e BUG-THUMB-01; E05B corrige apenas a moldura recortada.
2. Construir **índice de cenas pré-verificado**, por título + mídia (filme/episódio) + *edição/corte* e intervalos de tempo. Cada intervalo referencia ator, personagem, origem dos dados, licença/permissão, versão e nível de revisão. Não inventar intervalos a partir da lista global de elenco.
3. Sincronizar o instante selecionado na miniatura com o intervalo correspondente. Prever cortes alternativos, abertura/créditos, fontes/versões diferentes, `seek` rápido, mudança de episódio, offset e item sem dados.
4. Piloto pequeno e verificável em conteúdos cujo índice tenha origem permitida e revisão humana. Resultado **NÃO TESTADO** até obter evidências.

**Fase B — interface contextual após validação**
5. Em miniatura Seekr, mostrar discretamente `Em cena`/atores vinculados ao instante; abrir detalhes de elenco apenas se houver dados verificados, sem sobrepor imagem ou prejudicar DPAD. Evitar consultas por cada movimento/foco; cache e debounce.
6. Em **Detalhes**, mostrar um **badge discreto `Em Cena` somente para o título e edição em que o índice estiver validado**. Não prometer o recurso universalmente. Se não existir índice compatível, ocultar o badge e manter Seekr normal.
7. Dados ausentes, ambíguos ou desatualizados ⇒ **não exibir nomes nem badge**. Um fallback para elenco geral pode existir claramente identificado como **Elenco do título**, nunca como “Atores nesta cena”.

**Fase C — expansão posterior, opcional**
8. Investigar pipeline offline/servidor de IA sobre material cuja análise seja legalmente permitida, com revisão de confiança e humana; **não executar reconhecimento facial universal diretamente no APK** e **não tornar a IA requisito para a 2.1.0**. Deploy, banco remoto, custos e proteção de dados exigem aprovação separada.

### 2.3 Probabilidades recuperadas — expectativas, não medições

| Dimensão | Estimativa da análise anterior | Limite importante |
|---|---:|---|
| Reaproveitar miniaturas/posição Seekr | 85–95% | Depende do código real e versões do player |
| Integrar visual/badge com DPAD | 90–95% | Não é garantia de UX homologada |
| Identificar atores via índice verificado | 70–85% | Cobertura e sincronização variam por filme/edição |
| Reconhecimento generalizado por IA | 40–60% | Baixa confiabilidade sem revisão humana |
| Clonar X-Ray universalmente | <30% | Não há API pública do X-Ray para esse uso; direitos/dados são obstáculo |
| Piloto de escopo estreito | 80–90% | Opinião de viabilidade, não taxa de acerto observada |

**Interpretação correta de “quase 100%”:** só publicar o badge num título/edição após **100% dos casos definidos no conjunto de homologação passarem**, e ocultar em qualquer caso sem evidência. Isso não prova precisão universal e não autoriza alegar 100% em conteúdo não testado.

### 2.4 Arquitetura mínima proposta

- **Índice:** `titleId` (TMDB/IMDb quando aplicável), `mediaType`, `season`, `episode`, `cutId`/fingerprint não invasivo, duração, `startMs/endMs`, créditos de elenco e fonte de dados.
- **Consulta:** resolver conteúdo/edição → buscar intervalo para o `positionMs` do Seekr → retornar `VERIFICADO / INDISPONÍVEL / INCOMPATÍVEL` e dados de atores apenas no estado verificado.
- **Cache:** local, limitado e por edição; invalidação em mudança de fonte/episódio/edição, sem cruzar perfil e sem armazenar imagem biométrica. Se houver backend, permitir operação sem rede e manter gate de RLS/segurança.
- **UI:** botão/linha discreta integrada à miniatura, acessível por controle remoto; badge em Detalhes **condicional**; nenhum novo gerador de thumbs. Preservar contraste/sinopse, foco e desempenho da Fire de 32 bits.
- **Licenciamento:** não copiar dados proprietários de X-Ray, frames protegidos ou bases não licenciadas. Elenco TMDB geral **não comprova presença em cena**; IA/face recognition levanta riscos adicionais de privacidade e autorização.

### 2.5 Aceite para *permitir* badge

- [ ] E00 confirma o componente Seekr/gerador e estado real `JÁ EXISTE/PARCIAL/AUSENTE`.
- [ ] Miniatura existente sem corte, sincronizada com o instante, busca/pausa/retorno sem regressão.
- [ ] Índice autorizado e validado por **edição**, com proveniência e casos de teste reais.
- [ ] Não aparecem nomes falsos ao trocar fonte, corte, episódio ou ao buscar entre cenas.
- [ ] Badge somente nos Detalhes dos títulos validados; ausência do índice não torna o player defeituoso.
- [ ] DPAD, primeira carga, cache frio/quente, offline, queda de rede, RAM, latência e Fire/Shield verificados.
- [ ] Screenshots reais confrontados com layout aprovado; nenhum segredo/biometria em logs.
- [ ] Decisão de inclusão na 2.1.0 depende de gate: **piloto com PASS real**; se FAIL/NÃO TESTADO, retirar o badge da versão e deixar a expansão para uma atualização posterior.

## 3. OUTRAS MELHORIAS RECUPERADAS — SEM DUPLICAR A IMPLEMENTAÇÃO

| Requisito | Pedido consolidado | Estado da informação / dono |
|---|---|---|
| `DP21-GRID-META-01` | Grade/prévia lateral **apenas séries**: ano inicial–final ou aberto (ex.: `2000–`), ícone de calendário, classificação com ícone, **total confiável de episódios** (não confundir com temporadas), badge `Em exibição`/`Finalizada`/`Cancelada`; `Renovada` somente se confirmado. Não inventar status; evitar requisição TMDB em cada foco. | Complemento de **MUD-007B/MUD-008**, dono **E05**. `MetaPreview.kt`, `DanPlusFocusDock.kt`, `TmdbMetadataService.kt`, `TmdbApi.kt` precisam conferência E00. APK não testado. |
| `DP21-PAUSE-CREDIT-01` | “Você está assistindo”: **retirar Writer**, preservar Direção e Elenco (fotos/personagens quando existentes). | Leitura histórica indica `PauseOverlay.kt` já com Direção/Elenco; **verificar em E00** antes de modificar. Dono de QA **E07B**. |
| `RSQ-SCOUT-01` | Evoluir Scout/Surpreenda-me para experiência **Descubra-me** inspirada em FlickPick, com filtros contextuais, preferências, prevenção de repetições e boa UX. | **Não criar segundo motor Scout**; avaliar delta do fork Nuvio-ReSeq, dono **E07B**, dependências visuais E05 e progresso E06. |
| `RSQ-CALENDAR-01` | Calendário: manter o DanPlus como base, trazer apenas navegação/organização mais elegantes do ReSeq se superiores, preservar cinema/digital/Blu-ray e progresso de séries. | Dono **E06**; revisão visual E05, sem novo Calendário. |
| `LOGO-PTBR-01` | Descobrir/Biblioteca: logos TMDB pt-BR primeiro, fallback **idioma original oficial**, nunca inglês automático. | Já planejado, **E05**; conferir cache e primeiro foco. |
| `BUG-BACKDROP-01` | Indicações/Descobrir/Biblioteca: backdrop carregar ao primeiro foco, sem entrar em Detalhes e voltar. | Já planejado, **E05**, mesmo carregador/cache. |
| `BUG-THUMB-01` + `RS-04` | Corrigir recorte inferior da miniatura Seekr, **preservando gerador atual**. | **E05B**, bloqueador visual de `DP21-EMCENA-01`. |
| `REQ16-14` | Trailers YouTube dublados/legendados PT-BR, fallback e cache seguro no backend, sem chave pública. | **E09**, trilha independente; não duplicar. |
| `REQ16-09` / E08 | Indicações, backups Supabase, privacidade e RLS por conta/perfil. | **E08**, gate de segurança e produção separado. Índice “Em Cena” não deve disparar migration/deploy por conta própria. |

**Fora do APK DanPlus:** a correção do **add-on de ordens cronológicas** (Olhos de Wakanda, Loki, passagem episódio-a-episódio e filmes intercalados) pertence ao add-on **Fontes**, não ao DanPlus. Não misturar os pacotes.

## 4. ORDEM CONSOLIDADA DE EXECUÇÃO — SEM BUILD POR MICROTAREFA

| Ordem | Pacote/dono único | Entrada e resultado esperado |
|---|---|---|
| **E00** | ChatGPT normal GPT-6 Alto + Commander somente leitura | Revalidar PC/Git/AGENTS/fonte 2.0.2; mapear 146 decisões, MUD, REQ16, RS e adendos; encontrar implementado/parcial/ausente; nenhuma escrita/build. |
| **E01** | Codex Alto, condicional | Corrigir crash Detalhes **somente se reproduzido**. |
| **E02** | Codex Alto | Fontes e recuperação inteligente. |
| **E04** | Codex Alto | Range, arquivos grandes, memória/cache Fire; sem mexer em áudio. |
| **E03** | Codex Alto | Exo/MPV, formatos, áudio HD, Dolby/DTS, hardware Fire/Shield. |
| **E05A** | Gemini leitura → Codex Alto único escritor | Imagens/Focus Dock/Vitrine/Glass; logo PT-BR, backdrop, grade lateral séries; screenshots reais. |
| **E05B** | Codex Alto | Seekr e miniaturas: moldura sem corte, sincronização, DPAD, regressão. |
| **E05C** | ChatGPT planejamento + Codex laboratório | **`DP21-EMCENA-01`: somente infraestrutura e piloto de índice verificado**. Badge e UI só se a homologação do piloto passar. Sem IA facial obrigatória. **Não pode bloquear toda a 2.1.0** se inviável. |
| **E06** | Codex Alto | Progresso, Trakt/Calendário, séries/episódios. |
| **E07A** | Gemini leitura + mudanças isoladas com controle | Marca/textos PT-BR sem alterar créditos/legados. |
| **E07B** | Codex Alto | Scout/Descubra-me, Pause Direção/Elenco, legendas, perfis, DPAD. |
| **E08** | Codex Alto LAB | Relatórios/indicações/backups/RLS; qualquer backend para “Em Cena” somente com gate próprio. |
| **E09** | Codex Alto | Trailers PT-BR e cache isolados. |
| **E11** | ChatGPT arquitetura + execução com gate | Monitor Original/ARC e QA automatizado opcional; sem automerge. |
| **E10** | Gradle/ADB mecânicos + QA real | Um marco de integração/build final após codificação; smoke Fire/Shield, assinaturas/OTA, regressão e aprovação de release. |

**Dependência obrigatória:** `E05A → E05B → E05C`; `E05C` só acompanha a versão 2.1.0 se houver evidência de dados/sincronia e autorização específica para o patch. Todos os demais gates mantém a ordem nominal do roteiro de execução. Não permitir agentes diferentes editando o mesmo componente ao mesmo tempo.

## 5. CONTINUIDADE EXATA / STATUS EM 22/11/2026

**Feito de fato nesta retomada:**
- Recuperadas decisões-chave da conversa anterior; lidos plano e roteiro disponíveis na Biblioteca; conferidos `GEMINI.md` e `PLANO_DANPLUS_2_1_0.md` atuais via conector GitHub (somente leitura).
- Identificada a ausência do requisito `DP21-EMCENA-01` nesses arquivos do GitHub; harmonizados requisitos, vínculo, sequência e critérios de aceite neste documento.
- Tentativa de criar branch `docs/danplus-21-emcena-20261122` no GitHub **FAIL (HTTP 403, Resource not accessible by integration)**. Não houve commit remoto. **Não usar a main como atalho para contornar o bloqueio.**

**Não executado / NÃO TESTADO:** edição Kotlin/Compose, backend/DB/IA, edição GitHub, pull request, Gradle, APK 2.1.0, instalação, Seekr real, fotos/smoke, testes físicos e apresentação de badge no app.

**Próximo passo exato:** com acesso de escrita a branch aprovado e corrigido, incorporar este adendo ao repositório oficial em branch documental e inserir duas referências curtas em `GEMINI.md` e `docs/danplus/PLANO_DANPLUS_2_1_0.md`, revisando diffs sem mexer no código. Só depois executar **E00** em modo leitura com Commander, gerar matriz de presença por requisito e decidir se a fase piloto “Em Cena” cabe em E05C. Não iniciar patch/build até autorização específica.
