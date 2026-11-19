# DANPLUS · MATRIZ MUD R3, DEMANDAS POSTERIORES E ANTIDUPLICAÇÃO
Data de consolidação 17/11/2026. Não aplicar mudanças deste quadro sem conferência da fonte canônica e do código atual.

## O que foi entregue na versão 2.0.0 (conforme INICIAR_AUDITORIA e checkpoint posterior)
- Identidade DanPlus, dois APKs no mesmo commit 0ce90a0 / versionCode 1642, mantendo applicationIds e certificados diferentes para in-place.
- MUD-003 avatares, MUD-004 navegação Nuvio Original/DanPlus, MUD-005 relógio, MUD-007A Vitrine fixa, MUD-007B Grade com Elenco/Direção, MUD-008 Focus Dock, MUD-009 Onde Assistir, MUD-010 episódios V2: código relatado presente. **Não reimplementar**, priorizar QA visual e correção de discrepância real.
- MUD-002 lista local e relatórios, painel privado/respostas: relatório de backend/protótipo, implantação e QA separados; MUD-006 Indicações com código e migração local pronta, remota NÃO implantada; MUD-012 Destaque Especial por perfil com SQL/rollback local PASS, remoto NÃO implantado; MUD-011 credenciais/Seekr bloqueada por termos do provedor; MUD-013 Seekr validação/preview implementadas parcialmente, comportamento visual NÃO TESTADO.
- Adendos ARC historicamente incluídos em 2.0: player, memória, Home, Scout, Calendário, legendas e fontes. Pré-check obrigatório em vez de aplicar de novo. Condicionais ARC-P05, ARC-S06, ARC-DV01 e ARC-X01 seguem HOLD até testes e gates.

## Quadro MUD: dona do componente, ação correta
| Fonte | Função | Diretriz para Codex | Lote |
|---|---|---|---|
| MUD-001 | Marca e versão | Preservar APK 2.0.0 e package; no plano OTA 2.0.1, nome do app só DanPlus, sem “2.0” visível | DP-ML09 |
| MUD-002 | Relatórios e respostas | Validar fluxo local; estudar painel/ownership sem implantar backend | DP-ML06/07 |
| MUD-003 | Avatares | JÁ FEITO; smoke real | DP-ML06 |
| MUD-004 | Nuvio Original/DanPlus | JÁ FEITO; checar compatibilidade de preferências antigas | DP-ML06 |
| MUD-005 | Relógio | JÁ FEITO; validar fim da barra superior, fuso/12h/24h e DPAD | DP-ML06 |
| MUD-006 | Indicações e destinatários | Validar código e fotos/foco; SQL remoto aguardando autorização | DP-ML06/07 |
| MUD-007A | Vitrine fixa | JÁ FEITO; comparar foto e navegação DPAD | DP-ML06 |
| MUD-007B | Grade e elenco/direção | JÁ FEITO; medir texto/clipping no Fire | DP-ML06 |
| MUD-008 | Focus Dock Descobrir/Biblioteca | Implementado; foto homologada original precisa conferência antes de alterar | DP-ML02/06 |
| MUD-009 | Onde Assistir | Implementado; QA do tamanho e dos créditos | DP-ML06 |
| MUD-010 | Episódios V2 | Preservar Shield V2 e Fire Classic; teste físico | DP-ML06 |
| MUD-011 | Chave Seekr e sincronização protegida | **HOLD** até política provedor/RLS/segurança e gate humano | DP-ML07 |
| MUD-012 | Destaque Especial por perfil | Android/SQL local pronto; gate de migração, contas e conflito offline | DP-ML07 |
| MUD-013 | Seekr, botão Testar, prévia | Código parcial; foto e progresso por DPAD, BUG-THUMB-01 distinto | DP-ML02 |

## Pedidos posteriores da lista do proprietário (16/11/2026): REQ16, NÃO confundir com MUD-001…013
Esta é transcrição resumida de contexto de conversa, NÃO verificação recente do código nem aprovação automática para implementar. Antes de patch, Codex compara a especificação/foto do usuário, estado atual e registra a decisão final. Onde existe funcionalidade, corrigir somente diferença.
| ID | Pedido registrado | Próximo tratamento |
|---|---|---|
| REQ16-01 | Nome APK **DanPlus**, sem marca anterior | Convergir no plano ML09; técnica 2.0.1 |
| REQ16-02 | Aparência > Artes: remover Cartões/Cartão com letreiro, manter Clássico; retirar fundo transparente e usar banners próprios | Conferir UX atual; preservar escolhas homologadas, ML06 |
| REQ16-03 | Calendário: séries desaparecem | Reproduzir antes de patch, ML03 |
| REQ16-04 | Seekr: botões em linha; status verde apenas testado/válido | Revisar com MUD-013, ML02 |
| REQ16-05 | Descobrir/Biblioteca: logos/backdrops ao focar; buscar logo TMDB em português brasileiro primeiro, original somente na ausência; comportamento Shield vs Fire | Validar Focus Dock e prioridade de idioma em ambos sem duplicá-lo, ML02; ver adendo LOGO-PTBR-01 |
| REQ16-06 | Long-press OK: indicar para perfis lado a lado; logo centralizado, botão Enviar e sem Confirmar | Conferir MUD-006, ML06 |
| REQ16-07 | Verificação Seekr: mostrar flag no playback | MUD-013/ML02, status deve refletir uso real |
| REQ16-08 | Estatísticas de reprodução: PT-BR | Revisar strings e telas existentes, ML06 |
| REQ16-09 | Backup Supabase (contém referência histórica a chaves) | Somente auditoria de armazenamento e permissões; **NÃO copiar, imprimir nem distribuir segredos**, HOLD ML07 |
| REQ16-10 | Para Você: mudar “scout”, Mostrar assistido não opera; escurecer card com ícone | Reproduzir UI atual e não confundir filtro com regra do banco, ML06 |
| REQ16-11 | Avaliar/revisar recomendações específicas do dispositivo | Especificação insuficiente para patch automático; ML00 INVESTIGAR |
| REQ16-12 | Botão “i” precisa rolagem | Reproduzir em aparelho, ML06 |
| REQ16-13 | Para Você: layout em grade lateral, seletor Glass/Vitrine | Comparar com aparências aprovadas; não implantar sem foto/gate, ML06 |
| REQ16-14 | Trailers YouTube fallback dublado/legendado PT-BR, cache remoto Supabase, exemplo Minority Report | Missão própria ML08, fora do player/OTA; não confundir com A2-45 fallback IMDb |

## ADENDO 18/11/2026 — BUG-BACKDROP-01 · Imagens atrasadas em Indicações e Descobrir (EXIGÊNCIA PARA LOTE FUTURO)
**Origem:** relato atual do proprietário, sem inspeção de código, medição ou reprodução física nesta rodada. **Status: SOLICITADO / PLANEJADO; causa NÃO VERIFICADA; correção NÃO IMPLEMENTADA.**
**Sintoma reproduzível a investigar:** ao abrir **Indicações**, a imagem de fundo/backdrop do título indicado demora excessivamente a aparecer; em **Descobrir** (e verificar Biblioteca/Focus Dock quando reutilizar o mesmo componente), ao focar um card o backdrop também demora ou fica ausente. Entrar em **Detalhes** e retornar parece provocar o carregamento, mas **esse contorno manual não pode ser necessário**.
**Resultado obrigatório desejado:** ao abrir Indicações ou destacar um card em Descobrir, mostrar a imagem correta assim que disponível e atualizar a tela sem exigir entrada/saída de Detalhes; reutilizar cache útil, priorizar item atualmente focado, evitar imagem de outro título/perfil e preservar foco/DPAD e fluidez em Fire 32 bits e Shield.
**Vínculos sem duplicação:** MUD-006/REQ16-06 (fluxo Indicações), MUD-008/REQ16-05 (Focus Dock e imagens no foco), A2-32 (carregamento de pôsteres: continua **INVESTIGAR**, sem liberar patch automaticamente), A2-36 (artes), BUG-THUMB-01 (problema visual DIFERENTE, referente às miniaturas do player).
**Dono técnico único:** DP-ML02 investiga e, com evidência e gate, corrige compartilhamento de carregador, estado de UI/Compose, foco e cache de imagens. DP-ML06 apenas valida/ajusta peculiaridades do fluxo Indicações e outras telas já existentes, **sem segunda implementação do mecanismo de carregamento**. Nada a ver com cache de trailers no Supabase (DP-ML08), Seekr/miniaturas de playback ou OTA.
**Diagnóstico obrigatório na futura execução:** mapear evento de foco, disparo da requisição, resolução de URL/tamanho, download/decodificação, cache memória/disco e invalidação/recomposição da UI; comparar caminho direto versus após retorno de Detalhes. Verificar se é demora de rede, imagem indisponível, requisição não iniciada, cancelamento prematuro, key de cache incorreta, corrida de requisições, tela sem recomposição ou hierarquia/foco. Não atribuir uma causa antes de medi-la. Evitar múltiplos downloads idênticos, prefetch irrestrito, limpar cache global, aumentar RAM indiscriminadamente ou introduzir backend sem evidência.
**Critérios de aceite futuro:** testar cold start e cache quente, primeira seleção sem abrir Detalhes, troca rápida DPAD entre cards, ida e volta entre telas, perfis diferentes, rede lenta/falha e recuperação, imagens com proporções distintas. Registrar latência medida foco→backdrop visível e abertura de Indicações→backdrop visível (antes/depois, sem prometer número inventado), cache hit/miss quando observável, memória Fire, nenhum backdrop trocado ou preso, nenhum travamento de navegação. Comparar screenshot real Shield e Fire; fallback visual elegante enquanto imagem carrega; nunca depender do truque “abrir Detalhes e voltar”. Teste ausente = **NÃO TESTADO**, não PASS.
**Ordem de execução:** registrar agora apenas neste catálogo/roteiro; estudar no DP-ML00, atuar futuramente em DP-ML02 após gate DP-ML01 P0; realizar validação específica das Indicações no DP-ML06. Não alterar o APK nesta etapa documental.

## ADENDO 19/11/2026 — LOGO-PTBR-01 · Prioridade estrita de idioma no Focus Dock (REQ16-05)
**Origem:** relato do proprietário: atualmente os logos são buscados no TMDB, mas sem forçar PT-BR. **Status: SOLICITADO / DOCUMENTADO; código, consulta real ao TMDB e comportamento em aparelhos NÃO VERIFICADOS; NÃO IMPLEMENTADO.**
**Escopo EXCLUSIVO:** logos dos cards informativos exibidos ao focar títulos nas telas **Descobrir** e **Biblioteca**, reutilizando o Focus Dock MUD-008/REQ16-05. Não confundir logo textual com backdrop, pôster, sinopse, áudio, legenda ou título traduzido. Não alterar o layout homologado.
**Regra obrigatória:** 1) procurar **primeiro** logo em português brasileiro (preferência `pt-BR`); 2) **somente se não houver logo correspondente**, procurar no **idioma original oficial da obra**, consultando metadados da obra (`original_language`, e regionalização quando aplicável); 3) se nenhum dos dois existir, apresentar fallback visual neutro/sem logo, **não usar inglês ou outro idioma automaticamente**, salvo quando for o original. Verificar como o TMDB identifica o idioma de imagens (p.ex. `pt` versus `pt-BR`) antes de codificar; não inferir disponibilidade apenas do idioma da API. Não deixar logo sem idioma (`null`) ou ordenação arbitrária superar PT-BR.
**Cache:** separar seleção por ID da obra/tipo e idioma do logo; nunca substituir imagem em PT-BR por fallback de idioma original devido a resposta tardia, cache antigo ou mudança rápida de foco. Se o original for exibido enquanto se confirma PT-BR, atualizar para PT-BR quando localizado; respeitar consumo limitado de memória Fire.
**Diagnóstico antes do patch (DP-ML00/02):** rastrear componente Focus Dock, consulta TMDB e regra `include_image_language`/metadados, prioridade real das imagens e chave do cache; verificar se funcionalidade já existe parcialmente. Não duplicar carregador nem introduzir backend/Supabase.
**Aceite posterior (DP-ML02):** casos com logo PT-BR + original, apenas original, nenhum dos dois, obras originais em inglês/português/outro idioma; Fire e Shield, frio/quente, troca rápida DPAD e retorno de Detalhes. Capturas reais devem confirmar a prioridade; sem mock isolado como prova de APK. Status até validação: **NÃO TESTADO**.
**Antiduplicação:** mesmo REQ16-05, MUD-008 e DP-ML02; BUG-BACKDROP-01 trata de tempo de carregamento, não de idioma. Nenhum ID novo em 146/RS; nenhuma alteração no APK, OTA ou produção nesta etapa.

## Regras de consistência
- A lista REQ16 é camada de requisitos posteriores, não expande 146 IDs nem os RS. Não duplicar MUD-013 x REQ16-04/07 x BUG-THUMB-01; MUD-006 x REQ16-06; MUD-008 x REQ16-05; MUD-001 x REQ16-01/OTA; B1-27 x B2-07 x RS-03; histórico A1-22 x A2-38.
- Antes de alterar, conferir decisões atualizadas e arquivos da MUD em D:\APK\Evidencias\DANPLUS_2_0_ML00_R3_20261115; lista de 14 pastas originais e imagem homologada não foram relidas integralmente por esta preparação.
- Adotar/Adaptar/MANTER/INVESTIGAR são diretrizes de decisão, não prova de bug nem autorização de backend. Sem suprimir funcionalidades de perfis e assinaturas.
- Estado desta matriz: documental, **NÃO TESTADO** no dispositivo nesta preparação.