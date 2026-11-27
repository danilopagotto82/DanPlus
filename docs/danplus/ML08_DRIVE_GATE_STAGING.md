# RS-29 — Gate de staging e homologação

Use o projeto existente DANPLUS-SHIELD-STAGING (`wnzmojaliguncdbtuula`). O Drive guarda as revisões criptografadas. O Supabase intermedeia o OAuth e mantém somente contadores operacionais por proprietário e operação. A sincronização cobre gênero favorito, atalho no menu, emojis e exibição de assistidos do Descubra-me. O backup integral continua no fluxo manual.

## Configuração pelo proprietário

1. No projeto **DanPlus Drive Test** da Google Auth Platform, selecione o cliente **TV e dispositivos de entrada limitada**. Confirme que a API Google Drive está habilitada e que o acesso a dados inclui `https://www.googleapis.com/auth/drive.file`. Se o público for externo e estiver em **Teste**, adicione somente as contas Google de homologação em **Público-alvo → Usuários de teste**. Para esse escopo, grants de um aplicativo externo em teste podem expirar após sete dias. [OAuth para TV](https://developers.google.com/identity/protocols/oauth2/limited-input-device) · [Expiração de refresh tokens](https://developers.google.com/identity/protocols/oauth2#expiration)
2. Abra **Edge Functions → Secrets** no projeto staging. Informe os quatro campos abaixo diretamente no painel. Use o segredo ativo do cliente criado para TV. Se a cópia local tiver sido perdida, o console pode exibi-lo apenas no ato de criação de um novo segredo. Preserve o anterior até confirmar a migração de uso. [Painel de segredos do staging](https://supabase.com/dashboard/project/wnzmojaliguncdbtuula/functions/secrets) · [Documentação do Supabase](https://supabase.com/docs/guides/functions/secrets)

| Nome no Supabase | Valor |
| --- | --- |
| `DANPLUS_DRIVE_CLIENT_ID` | ID do cliente OAuth Google do tipo TV |
| `DANPLUS_DRIVE_CLIENT_SECRET` | Segredo ativo desse mesmo cliente, obtido em canal privado |
| `DANPLUS_DRIVE_TICKET_SECRET` | Segredo independente, gerado localmente ou no cofre com pelo menos 32 caracteres aleatórios |
| `DANPLUS_DRIVE_ALLOWED_OWNERS` | UUIDs dos proprietários efetivos retornados por `get_sync_owner` para as contas de teste Nuvio, separados por vírgulas |

O Supabase injeta `SUPABASE_URL` e `SUPABASE_SERVICE_ROLE_KEY` no servidor. O build de teste precisa somente de `DANPLUS_D12_SUPABASE_URL=https://wnzmojaliguncdbtuula.supabase.co` e `DANPLUS_D12_SUPABASE_KEY` com a chave **publishable** `sb_publishable_` do projeto, configuradas no `local.dev.properties` ignorado pelo Git da cópia efetiva. Obtenha a chave em **Settings → API Keys**. O broker exige login no backend oficial Nuvio e perfil remoto sincronizado. [Chaves do staging](https://supabase.com/dashboard/project/wnzmojaliguncdbtuula/settings/api-keys)

## Aplicação e deploy após autorização específica

Em 27/11/2026, o proprietário autorizou estes dois passos no staging. O Supabase registrou o SQL do arquivo local `20261127195424` como migration remota `20261127201623` e publicou `danplus-drive-auth` v1. A migration já consta no histórico remoto. No teste inicial, GET retornou 405 e POST anônimo retornou 503 `not_configured`. Depois de o proprietário salvar os campos, o POST anônimo retornou 401 `sign_in`, com `Cache-Control: no-store`. Isso confirma a aceitação da configuração mínima e a recusa de pedidos sem login. Ainda é preciso comprovar a admissão de uma conta Nuvio de teste e o consentimento Google. O host e a chave publicável de staging estão no arquivo privado da cópia efetiva.

1. No SQL Editor do staging, confira projeto e objetos alvo. Aplique exclusivamente o arquivo [20261127195424_danplus_drive_auth_admission.sql](../../supabase/migrations/20261127195424_danplus_drive_auth_admission.sql) e registre a execução. A migration cria um schema privado e um RPC com execução restrita ao `service_role`. Teste chamadas negadas e permitidas usando clientes fictícios e confirme concorrência com conexões independentes na mesma janela.
2. Publique somente a função `danplus-drive-auth`, com a validação de JWT do gateway desativada para esta função. O JWT vem do Nuvio e a função autentica o usuário com o emissor oficial, resolve o proprietário e verifica a lista de admissão a cada requisição. Use o comando a partir da raiz do checkout com a sessão da CLI autenticada no Supabase. [Deploy por CLI](https://supabase.com/docs/guides/functions/deploy)

   ```powershell
   npx supabase functions deploy danplus-drive-auth --project-ref wnzmojaliguncdbtuula --no-verify-jwt
   ```

3. Verifique primeiro que requisições sem autenticação ou de proprietário fora da lista são recusadas. Em caso de reversão autorizada, retire somente esta função e aplique o [rollback correspondente](../../supabase/rollback/20261127195424_danplus_drive_auth_admission.sql). Ele remove somente o RPC, a tabela de contadores e o schema específico. Os arquivos sobre os quais o Google concedeu acesso permanecem no Drive.

## Homologação em dois aparelhos

Em cada aparelho, abra **Configurações → Backup e restauração → Google Drive** com o mesmo proprietário de teste e perfil remoto. Use o mesmo PIN de 12 a 16 dígitos. Faça o consentimento com o código exibido e uma conta Google de teste.

| Cenário | Resultado esperado |
| --- | --- |
| Shield e Fire TV, primeiro vínculo | O código abre domínio Google. Os controles recebem foco por DPAD. Voltar cancela o fluxo e devolve foco ao cartão. |
| Abertura e preferência modificada na outra TV | A outra TV recebe a mudança por atualização manual ou pelo intervalo de cinco minutos em primeiro plano, com sincronização automática ligada. |
| Reinício e automático desligado | Último sucesso reaparece para o perfil vinculado. Uma alteração não dispara envio até solicitar **Sincronizar agora**. |
| Offline, online e token expirado | Mudança local é preservada. Após retorno da rede, a TV retoma com o mesmo perfil ou pede reconexão se o grant perdeu validade. Confirmar renovação direta do token com o cliente TV registrado. |
| Dois perfis e troca de conta durante espera | Código, conflito, grant, status e preferências continuam no escopo correto. Operações antigas deixam de ser aceitas. |
| Edições simultâneas e PIN incorreto | O conflito pede escolha explícita. PIN incorreto deixa as preferências e o histórico intactos. |
| Capacidade de 256 em dados de teste | Interface informa o limite e recusa novas escritas quando já há 256 revisões visíveis. As revisões existentes são preservadas. Duas TVs podem ultrapassar o limite em corrida. Não há compactação habilitada. |
| Desconexão local, revogação e novo vínculo | Desconexão afeta esta TV. Revogação exige confirmação, pode afetar outras TVs e preserva arquivos. Validar o retorno e reconexão. |

Execute também a regressão de backup manual e o checklist de reprodução por título e aparelho. Submeta a versão prevista `2.1.2` e `versionCode 1647` somente após o gate de compilação, as duas TVs e a verificação dos certificados e OTA. [Checklist físico ML08](CHECKLIST_FISICO_ML08.md)

## Correção isolada da renovação OAuth

O ensaio real informou PASS em login Nuvio, admissão, consentimento Google, escopo e identidade inicial. A renovação direta recebeu HTTP 400 `invalid_request client_secret`. O cliente atualizado renova pela mesma Edge Function. A operação `refresh` exige sessão Nuvio e um comprovante HMAC emitido depois do consentimento. O comprovante vincula ator, proprietário, cliente OAuth, escopo e token do grant. Um token substituído não passa na validação. O broker usa o segredo já armazenado no servidor, reaproveita o orçamento persistente de seis inícios e renovações por proprietário a cada dez minutos e responde sem cache. Revogação ou expiração do grant pede reconexão. Revisões e preferências permanecem preservadas.

Após autorização específica, publique somente a função pelo mesmo comando anterior. Aproveite a migration e os segredos existentes. Ative o cliente atualizado somente após a função emitir `refresh_proof`. Grants anteriores ao comprovante exigem uma nova confirmação Google, com a mesma conta e o mesmo PIN. Para repetir a prova, use `obter_uuid_nuvio.py --testar-broker` e as mesmas contas de teste, confirme `drive.file` e compare a identidade Google antes e depois da renovação. O assistente não grava arquivos no Drive. Se for necessário reverter o deploy, recupere somente a função do checkpoint anterior `89be3f8`, sem reverter a migration ativa. [OAuth para TV](https://developers.google.com/identity/protocols/oauth2/limited-input-device) · [RFC 6749, renovação](https://datatracker.ietf.org/doc/html/rfc6749#section-6)

O proprietário autorizou o deploy isolado e a v3 ficou ativa no staging. `start` e `refresh` passaram no smoke anônimo com 401 e `no-store`. O ensaio autenticado com a conta de teste passou em consentimento, escopo drive.file e renovação pela v3. O permissionId permaneceu estável após a troca de token e o ensaio não gravou arquivos no Drive. A homologação entre aparelhos, a revogação real e a concorrência continuam pendentes.
