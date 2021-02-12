// Inspiration comes from https://codelabs.developers.google.com/codelabs/webauthn-reauth#2 but without the required libs o/

function submitTFA(type, data) {
    document.getElementById("type").setAttribute("value", type);
    document.getElementById("data").setAttribute("value", data);
    document.forms.tfaForm.submit();
}

const webauthnChallenge = async () => {
    if (!navigator.credentials) {
        $('#error-text').innerText = "Votre navigateur ne supporte pas l'utilisation de clés de sécurité.";
        $('#error').alert();
        return;
    }

    if (!window.fetch) {
        $('#error-text').innerText = "Votre navigateur ne supporte pas l'utilisation des API Javascript récentes.";
        $('#error').alert();
        return;
    }

    // Fetch
    const response = await fetch("/login/tfa/webauthn/start ")

    let contentType = response.headers.get("content-type");
    if (contentType && contentType.indexOf("text/json") !== -1) {
        const json = await response.json()

        const pk = json.pk.publicKeyCredentialRequestOptions;
        pk.challenge = base64url.decode(pk.challenge);
        for (let cred of pk.allowCredentials) {
            cred.id = base64url.decode(cred.id);
        }

        const creds = await navigator.credentials.get({publicKey: pk});
        console.log(creds);

        const credential = {};
        credential.id = creds.id;
        credential.type = creds.type;
        credential.clientExtensionResults = {};

        if (creds.response) {
            const clientDataJSON = base64url.encode(creds.response.clientDataJSON);
            const authenticatorData = base64url.encode(creds.response.authenticatorData);
            const signature = base64url.encode(creds.response.signature);
            // const userHandle = base64url.encode(creds.response.userHandle);

            credential.response = {
                clientDataJSON,
                authenticatorData,
                signature,
            //    userHandle,
            };
        }

        const payload = {
            uid: json.uid,
            pk: credential
        }

        submitTFA('WebAuthn', JSON.stringify(payload));

    } else {
        $('#error-text').innerText = "Une erreur réseau s'est produite, merci de réessayer.";
        $('#error').alert();
    }
}

document.getElementById("use-webauthn").onclick = webauthnChallenge
