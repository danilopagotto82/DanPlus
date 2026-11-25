# CHECKPOINT FREEZE-R1 — DanPlus 2.1.0

## Base e correções

- Worktree `D:/APK/Worktrees/DanPlus_2_1_0_E02_20261125` na branch `feat/danplus-2.1.0-integracao-20261125`.
- HEAD inicial local e remoto `1b577f0089fb968c63d1390b2ef5275525c6d80b`. A main remota foi conferida em `04e5b4e1780c43f6de4e5627453985821c07d48b`. Worktree limpa no precheck.
- **ASS.** `PlaybackSettingsOrganizationTest` fornece um editor de teste identificável e verifica sua posição em Legendas. Mantidas as verificações de organização existentes.
- **Android TV Channel.** `AndroidTvChannelSyncServiceTest` injeta `ProfileManager`, usa snapshots explicitamente vinculados ao perfil e abre uma sessão real de `PlayerSessionTracker`, fechada no teardown. Exercita troca de perfil durante leitura e entre a escrita do canal e a do Watch Next.
- **PostPlay.** Três casos de `PostPlayRecommendationSchedulingTest` e seis de `PostPlayWatchedHistorySchedulingTest` ajustados para provar ausência de consulta antes de `playbackEnded`, com os critérios anteriores de seleção, reprodução e histórico. Quatro regressões adicionais cobrem retomada do filme durante busca e contador, início manual antes de o observador receber a retomada, troca de recomendação e resposta tardia durante A → B → A.
- **Patch funcional focal.** `PostPlayRecommendationController` deixava o contador pós-fim continuar após retomada do filme. Uma contagem da reprodução anterior podia provocar início imediato no próximo término. O patch cancela a contagem, exige o estado atual do player para consultar, publicar ou iniciar trailer e admite uma nova contagem de cinco segundos após o término. Busca de recomendações, avaliações e guardas de histórico continuam com seus contratos.
- **Supabase 3.2.1 — PASS de compatibilidade estática.** As fontes locais e o bytecode dos AARs release confirmam `Postgrest.Companion.createConfig` e `create(SupabaseClient, Config)`. `MainConfig.jwtToken` é repassado pelo plugin ao cliente autenticado e tem precedência sobre o provedor de token, a sessão corrente e a chave de fallback. `ProviderCredentialSyncService` permanece com JWT por operação e guardas E08.

## OTA e ambiente

O maior `versionCode` publicado é **1644**. A API GitHub retornou HTTP 200 sem autenticação em ambas as origens. O legado tem cinco releases publicadas com códigos 1639–1644. O repositório novo não tem releases publicadas. Os dois APKs locais 2.0.2 têm SHA-256 igual ao dos respectivos assets publicados e o `aapt` confirmou 1644, pacotes e ABIs. Logo, **1645** é superior ao máximo observado. Revalidar perto da compilação autorizada, mantendo os defaults 2.0.2/1644 até o gate de release. [Releases legadas](https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases) · [Releases DanPlus](https://github.com/danilopagotto82/DanPlus/releases)

| Variante | Pacote e ABI preservados | Nome local final |
|---|---|---|
| `assembleFullRelease` | `com.danplus.shield` · `arm64-v8a` | `app-full-arm64-v8a-release.apk` |
| `assembleFullFireFireOta` | `com.danexus.tv.fire.debug` · `armeabi-v7a` | `DanPlus-FireTV-2.1.0-armeabi-v7a.apk` |

O código atual consulta o legado primeiro e valida URLs de assets por origem. O nome Shield é exato e o regex Fire aceita prefixos `DaNexus` e `DanPlus`, com preferência pelo legado quando ambos existem. O mesmo regex de transição foi localizado no DEX do Fire 2.0.2 publicado. A compatibilidade com clientes anteriores ao 2.0.2 e o fluxo de download/instalação em dispositivos continuam não testados. Manter o canal legado ativo para a base instalada.

- **Ferramentas verificadas.** JDK 21.0.11, plataforma Android 36, build-tools 36.0.0, NDK 29.0.14206865, CMake 3.22.1, wrapper 8.13 e distribuição local. Libdovi e cabeçalhos presentes nas duas ABIs, com objetos AArch64/ARM. Os 12 AARs locais declarados têm `classes.jar` e os que contêm JNI incluem ambas as ABIs. Cerca de 57,7 GiB livres em C: e 187,7 GiB em D: no precheck.
- **Obrigatório no E10.** `local.properties` ainda está ausente na candidata e é ignorado pelo Git. A configuração privada de referência existe, com SDK válido e configuração Supabase/D12 presente, e existe backup protegido. A configuração de referência não prepara a assinatura Shield. O `PSCredential` do cofre DPAPI foi aberto em leitura e o alias e a chave corresponderam ao certificado do APK Shield publicado. O keystore debug existente também corresponde ao Fire publicado. As quatro variáveis `NUVIO_RELEASE_*` não estavam carregadas. Nenhum segredo ou arquivo privado foi alterado.
- **Opcionais.** `TRAILER_API_URL` sem valor na referência inspecionada. O fallback existente permanece. WeTrakr sem Client ID autorizado continua pendente de homologação externa. A configuração vazia é admitida pelo Gradle. A chave YouTube retirada não foi consultada.
- **Rollback.** Preservar APKs 2.0.2 e evidências. Um downgrade para 1644 depende de validação de dados e instalação separada.

## Roteiro local proposto para E10

Executar somente depois de o coordenador revisar o FREEZE-R1 e autorizar a compilação. Definir `DANPLUS_E10_APPROVED_HEAD` com o commit aprovado. Revalidar o máximo publicado antes de usar 1645. Os comandos abaixo geram e conferem arquivos locais.

```powershell
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath 'D:/APK/Worktrees/DanPlus_2_1_0_E02_20261125'
if ($env:DANPLUS_E10_APPROVED_HEAD -notmatch '^[a-f0-9]{40}$' -or
    (git rev-parse HEAD) -ne $env:DANPLUS_E10_APPROVED_HEAD -or
    (git branch --show-current) -ne 'feat/danplus-2.1.0-integracao-20261125' -or
    (git status --porcelain=v1)) { throw 'Checkout E10 divergente' }
if ($env:CI_USE_DEBUG_SIGNING -eq 'true') { throw 'Shield exige a assinatura release original' }
if (-not (Test-Path -LiteralPath 'local.properties')) {
    git check-ignore --quiet -- local.properties
    if ($LASTEXITCODE -ne 0) { throw 'Configuração privada não está ignorada pelo Git' }
    Copy-Item -LiteralPath 'D:/APK/Worktrees/DanPlus_OFICIAL_FONTE_SEGURA_20261119/local.properties' -Destination 'local.properties'
}
$sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$tools = Join-Path $sdk 'build-tools/36.0.0'
$flags = @('-PdanplusUnifiedOta=true', '-ParcVersionName=2.1.0', '-ParcVersionCode=1645')

& .\gradlew.bat :app:testFullReleaseUnitTest @flags --tests 'com.nuvio.tv.core.sync.androidtv.AndroidTvChannelSyncServiceTest' --tests 'com.nuvio.tv.ui.screens.player.PostPlayRecommendationSchedulingTest' --tests 'com.nuvio.tv.ui.screens.player.PostPlayWatchedHistorySchedulingTest' --tests 'com.nuvio.tv.updater.ForkUpdateRepositoryTest' --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Testes focados E10 falharam' }
& .\gradlew.bat :app:compileFullDebugAndroidTestKotlin @flags --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Compilação dos testes instrumentados falhou' }

$signingDir = Join-Path $env:LOCALAPPDATA 'DanPlus/Shield/Signing'
$vault = @(Get-ChildItem -LiteralPath $signingDir -Filter '*.clixml' -File)
if ($vault.Count -ne 1) { throw 'Cofre Shield ausente ou ambíguo' }
$shield = Import-Clixml -LiteralPath $vault[0].FullName
if ($shield -isnot [PSCredential]) { throw 'Formato do cofre Shield inválido' }
$keys = 'NUVIO_RELEASE_STORE_FILE','NUVIO_RELEASE_KEY_ALIAS','NUVIO_RELEASE_STORE_PASSWORD','NUVIO_RELEASE_KEY_PASSWORD'
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key) }
try {
    $env:NUVIO_RELEASE_STORE_FILE = Join-Path $signingDir 'danplus-shield-release.p12'
    $env:NUVIO_RELEASE_KEY_ALIAS = $shield.UserName
    $env:NUVIO_RELEASE_STORE_PASSWORD = $shield.GetNetworkCredential().Password
    $env:NUVIO_RELEASE_KEY_PASSWORD = $env:NUVIO_RELEASE_STORE_PASSWORD
    & .\gradlew.bat :app:assembleFullRelease @flags --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build Shield falhou' }
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key]) }
    Remove-Variable shield
}
& .\gradlew.bat :app:assembleFullFireFireOta @flags --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Build Fire falhou' }

$outputRoot = 'D:/APK/Evidencias/DANPLUS_2_1_0_E10_1645'
if (Test-Path -LiteralPath $outputRoot) { throw 'Escolha um destino de evidências novo' }
$targets = @(
    @{ Variant='fullRelease'; Folder='full/release'; AppId='com.danplus.shield'; Abi='arm64-v8a'; Name='app-full-arm64-v8a-release.apk'; Signer='bb8b816273abe6f458d9b93ea3374bc657935d399b27169b634164d97f404950' },
    @{ Variant='fullFireFireOta'; Folder='fullFire/fireOta'; AppId='com.danexus.tv.fire.debug'; Abi='armeabi-v7a'; Name='DanPlus-FireTV-2.1.0-armeabi-v7a.apk'; Signer='77066ec7da0b758bbab4304a2ae64e22ed0f381d1ed29f85abfb1be3d26f1419' }
)
foreach ($target in $targets) {
    $folder = Join-Path 'app/build/outputs/apk' $target.Folder
    $metadata = Get-Content -Raw -LiteralPath (Join-Path $folder 'output-metadata.json') | ConvertFrom-Json
    $items = @($metadata.elements)
    if ($metadata.variantName -ne $target.Variant -or $metadata.applicationId -ne $target.AppId -or
        $items.Count -ne 1 -or $items[0].versionCode -ne 1645 -or $items[0].versionName -ne '2.1.0') {
        throw "Metadados divergentes em $($target.Variant)"
    }
    $apk = Join-Path $folder $items[0].outputFile
    $badging = & (Join-Path $tools 'aapt.exe') dump badging $apk
    $manifest = "name='$($target.AppId)' versionCode='1645' versionName='2.1.0'"
    if ($LASTEXITCODE -ne 0 -or -not ($badging | Select-String -SimpleMatch $manifest) -or
        -not ($badging | Select-String -Pattern "^native-code:\s+'$($target.Abi)'\s*$")) {
        throw "Pacote, versão ou ABI divergentes em $($target.Variant)"
    }
    $certs = & (Join-Path $tools 'apksigner.bat') verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw "Assinatura inválida em $($target.Variant)" }
    $signers = @($certs | Select-String '^Signer #[0-9]+ certificate SHA-256 digest:')
    if ($signers.Count -ne 1 -or -not $signers[0].Line.EndsWith($target.Signer)) {
        throw "Certificado divergente em $($target.Variant)"
    }
    $target.Source = $apk
}
New-Item -ItemType Directory -Path $outputRoot | Out-Null
foreach ($target in $targets) {
    $destination = Join-Path $outputRoot $target.Name
    Copy-Item -LiteralPath $target.Source -Destination $destination
    Get-FileHash -LiteralPath $destination -Algorithm SHA256
}
```

## Resultado e gates

- **PASS:** refs e escopo Git, `git diff --check`, revisão estática e PSI Kotlin 2.3.0 em cinco arquivos com zero erros de sintaxe. O roteiro E10 passou no parser PowerShell e está pendente de execução. Conferência da SDK, credenciais protegidas por presença/assinatura, publicação 1644 e disponibilidade das duas origens.
- **NÃO TESTADO:** execução JUnit/instrumentação, compilação/build, geração de APK, assinatura da candidata e smoke físico/OTA. Nenhum Gradle, push, merge, release, instalação, migração ou alteração remota. Defaults de versão, assinatura em código, application IDs, perfis e checkpoints anteriores preservados. E11 não iniciado.
- **Próxima ação:** revisão independente do commit local. Depois da autorização, executar os comandos de teste e empacotamento do E10, com smoke físico e publicação sujeitos aos respectivos gates.
