# SCP Wallet Android
SCP Wallet is lightweight [ScPrime](https://gitlab.com/scpcorp/ScPrime) wallet for Android.

## Get started
You can build the APK from source or download the latest release.

## Security
The data stored by the app is encrypted with [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) which takes advantage of the [Android keystore system](https://developer.android.com/training/articles/keystore).

Sensitive information such as the wallets' seeds and the addresses' private keys are further encrypted with [libsodium](https://github.com/jedisct1/libsodium)'s crypto secret box (XSALSA20 and POLY1305).

The app communicates with [scp-wallet-api](https://github.com/paolo96/scp-wallet-api) to transfer the following information:
* Get the transactions for a given set of addresses
* Get generic information such as current block height, fees and fiat price of SCP
* Broadcast a signed transaction to the network

Information that allows to spend funds does never leave the device, thus limiting the attacks options for a malicious server.

The server is chosen from a list of trusted servers hardcoded in the app and it can be changed by the user. It is advised to run your own *scp-wallet-api* instance for maximum security.

## Preview

<p float="left">
  <img src="https://user-images.githubusercontent.com/24766249/151459283-e55d7695-84fd-4f4d-ac87-b2f7bc0adaf2.png" width="49%" />
  <img src="https://user-images.githubusercontent.com/24766249/151459284-fe817abb-2d48-4d88-9b8e-f05a8e563453.png" width="49%" />
</p>
<p float="left">
  <img src="https://user-images.githubusercontent.com/24766249/151459274-aed9acf5-d4ee-4349-ad4f-e3de0425ac7c.png" width="49%" />
  <img src="https://user-images.githubusercontent.com/24766249/151459279-a0b23109-8c9d-46b9-bfa4-6f99f06d7686.png" width="49%" />
</p>

## Support the project

If you find SCP Wallet useful and you want to contribute to the project, there are multiple things you can do:

### Donations

You can donate to support future development:
* SCP -> 71b797c650193b125cd0042dd8ab0be9e4f549537bc061a17ce3dddca8983938b401c4b92b0c
* BTC -> 12EVpv75KnDKPuKuGgxedw3ad8VbR5mc8e

### Development

Feel free to make a pull request to add useful features or bug fixes.

### Run a *scp-wallet-api* server

If you're able to run a reliable server contact me to be added to the list of trusted servers.
