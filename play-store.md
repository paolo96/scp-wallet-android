# Play store listing information

TODO: add localized screenshots

## English

### App name

SCP Wallet (ScPrime)

### Short description

Lightweight, privacy focused, open source ScPrime Wallet

### Description

SCP Wallet is lightweight, privacy focused, open source ScPrime Wallet.
Create or import your SCP wallets in the app. Easily send and receive transactions on the ScPrime Blockchain.

** Open source

You can view the app code here:
https://github.com/paolo96/scp-wallet-android

** Server communication

The app communicates with a server to transfer the following information:
- Get the transactions for a given set of addresses
- Get generic information such as current block height, fees and fiat price of SCP
- Broadcast a signed transaction to the network
Information that allows to spend funds does never leave the device, thus limiting the attacks options for a malicious server.
The server is chosen from a list of trusted servers hardcoded in the app and it can be changed by the user. It is advised to run your own server instance (which is also open source) for maximum privacy.

** Encryption

The data stored by the app is encrypted with EncryptedSharedPreferences which takes advantage of the Android keystore system.
Sensitive information such as the wallets' seeds and the addresses' private keys are further encrypted with libsodium's crypto secret box (XSALSA20 and POLY1305).


## Italian

### App name

SCP Wallet (ScPrime)

### Short description

Portafoglio ScPrime leggero, focalizzato sulla privacy e open source

### Description

SCP Wallet è un portafoglio ScPrime leggero, focalizzato sulla privacy e open source.
Crea o importa i tuoi portafogli ScPrime all'interno dell'app. Invia e ricevi transazioni sulla blockchain ScPrime in modo semplice.

** Open source

Puoi vedere il codice open source qui:
https://github.com/paolo96/scp-wallet-android

** Comunicazione col server

L'app comunica con un server per trasferire le seguenti informazioni:
- Ottere le transazioni data una lista di indirizzi
- Ottenere informazioni generiche come l'attuale altezza della blockchain, le commissioni e i prezzi fiat di SCP
- Trasmettere una transazione firmata al network ScPrime
Le informazioni che permettono di spendere fondi non lasciano mai il tuo dispositivo, limitando quindi le opzioni di attacco di un ipotetico attore malevolo.
Il server è scelto a caso tra una lista di server fidati scritti all'interno dell'app e può essere cambiato dall'utente. Infatti è consigliato utilizzare il proprio server (anche quello open source) per garantire il massimo livello di sicurezza.

** Crittografia

I dati salvati dall'app sono criptati con EncryptedSharedPreferences che utilizza l'Android keystore system.
Le informazioni sensibili come i seed dei wallet e le chiavi private degli indirizzi sono ulteriormente criptate con il crypto secret box di libsodium (XSALSA20 e POLY1305).
