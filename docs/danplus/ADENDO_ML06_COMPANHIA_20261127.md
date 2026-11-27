# ML06 - Complemento DP-S04: nenhuma companhia ignorada
Aprovado pelo proprietario em 2026-11-27. Base efetiva f65bc0b. PENDENTE de implementacao e testes.
ML05-CLOSE: preservar b5a05ee/f65bc0b e o aceite focal relatado (92 testes, 0 falhas). Nao remover cadastro opcional nem confirmacao de presenca ja entregues. Preservar RS-26 e DP-S10. Nao reabrir ML05.

## Regra de produto
- Se participante autorizado ja completou a configuracao de gosto do Surpreenda-me e tem sinais/pool utilizavel, usar o mecanismo Scout existente: avaliacoes, preferencias e recomendacoes por perfil. As cinco avaliacoes positivas iniciais devem ser um caminho para alimentar o pool; verificar o fluxo real e a atualizacao do cache. Nao assumir que cinco likes sozinhos garantem pool carregado.
- Se faltarem dados de gosto utilizaveis, oferecer a mesma selecao rapida de generos usada pelo convidado, identificando cada participante. Nao ignorar silenciosamente nenhum participante; permitir escolha explicita de sem preferencia, como afinidade neutra documentada.
- Reutilizar os generos TMDB existentes e rotulos PT-BR, com multisselecao; nao limitar a acao, comedia e drama. Nada de novo cadastro obrigatorio ou novo servico de IA. Preferencias da sessao ficam temporarias, sem alterar gostos ou historico de terceiros.
- Corrigir ScoutPicksFinder.kt (informedPools exclui pools vazios) e ScoutViewModel.kt (guest.genre atualmente promove genero global). Incorporar preferencias temporarias na afinidade individual de cada participante e equilibrar minimo 65% + media 35%, preservando nota de qualidade e exclusoes. Evitar dupla ponderacao e tratamento injusto do convidado. Sem mudanca de ranking individual fora do escopo.
- Manter PIN, isolamento, classificacao BR/limite mais restritivo, clima, duracao, rewatch explicito, recusas, fluxo de reproducao e presenca opcional existente. Sem gravar historico de terceiros.
- Sem consulta extra de IA para grupo; sem novos downloads de catalogos desnecessarios. Preservar DP-S04 e demais IDs existentes, registrar criterio complementar sem duplicar IDs canonicos.
- Validar cache atualizado, perfil com cinco avaliacoes iniciais, perfil sem pool, convidado com varios generos, dois participantes sem pool, sem preferencia explicita, conflito de gostos, crianca/PIN e troca de perfil. Testes focados, diff check. Sem APK intermediario.

## ML08 - checklist fisico
1. Interesse por Interestelar: perfil com gosto configurado e outro sem dados; abrir Minha sessao de cinema, selecionar companhia; abrir seletor apenas para quem precisa.
2. Garota Exemplar e Diario de uma Paixao: dados sinteticos de suspense e romance; comparar ranking em teste deterministico, sem exigir titulo especifico na TV.
3. Toy Story: convidado/participante com limite familiar; preservar classificacao BR e nao gravar dados de terceiros.
Nos dois aparelhos, registrar titulo realmente sugerido, perfis, generos, foco e PASS/FAIL/NAO TESTADO. Sem obrigar um resultado de filme especifico.

## Reconciliacao de decisoes do proprietario no ML06/07
- RS-29 antecipado para ML06, reaproveitar integracao ReSeq e backup existente. Sync automatico apenas lacunas nao cobertas por Nuvio/ARC; backup manual abrangente conforme politica. Preservar formato, compatibilidade, criptografia e isolamento de perfis. Nunca aplicar restore integral a cada sincronizacao. OAuth real somente gate autorizado.
- Seekr criptografado; Client ID WeTrakr ja configurado nos arquivos privados de build, nao incluir em backup. Verificar ambas variantes no ML08, sem imprimir valor. Ajustes tecnicos e resultado de avaliacao de dispositivo exclusivamente locais.
- Auditar as novas chaves de scout_cinema_private (vinculo, faixa etaria, onboarding e recibo de presenca), classificando responsabilidade e privacidade; nao sincronizar recibos automaticamente por presuncao.
- UI-13: ler ADENDO_ML06_UI13_20261127.md ja importado. Preservar controles e estender somente arte.
- LAB-EM-CENA: retirar da implementacao 2.1.2 por autorizacao do proprietario. Preservar ID, pesquisa e evidencias, registrar adiamento sem apagar dados. Elenco normal nos detalhes intacto. Nao gastar recursos em piloto X-Ray nesta versao.
- Preservar 34 IDs canonicos e 10 complementares DP-S. Atualizar matrizes/checkpoint no encerramento pelo executor unico; nao iniciar build ate ML08.