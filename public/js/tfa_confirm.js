// Inspiration comes from https://codelabs.developers.google.com/codelabs/webauthn-reauth#2 but without the required libs o/

function submitTFA(type, data) {
    document.getElementById("type").setAttribute("value", type);
    document.getElementById("data").setAttribute("value", data);
    document.forms.tfaForm.submit();
}

const webauthnChallenge = async () => {
    document.getElementById("error").hidden = true;

    if (!navigator.credentials) {
        document.getElementById("error-text").innerText = "Votre navigateur ne supporte pas l'utilisation de clés de sécurité.";
        document.getElementById("error").hidden = false;
        return;
    }

    if (!window.fetch) {
        document.getElementById("error-text").innerText =  "Votre navigateur ne supporte pas l'utilisation des API Javascript récentes.";
        document.getElementById("error").hidden = false;
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

        let creds = null;
        try {
            creds = await navigator.credentials.get({publicKey: pk});
        } catch (e) {
            console.log("Error while using security key:");
            console.log(e);

            document.getElementById("error-text").innerText = "Votre clé de sécurité n'est pas reconnue.";
            document.getElementById("error").hidden = false;

            return;
        }
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
        document.getElementById("error-text").innerText =  "Une erreur réseau s'est produite, merci de réessayer.";
        document.getElementById("error").hidden = false;
    }
}

// One of these two techniques (or both?) successfully sets the event "onclick" of the button to "webauthnChallenge"
// We have to do this because the CSP (ContentSecurityPolicy) doesn't allow onclick="..." in HTML (considered as inline JS)
// Allowing 'unsafe-inline' in the CSP would also fix this problem, but if we can avoid it it's better to avoid it.
document.getElementById("use-webauthn").onclick = webauthnChallenge
document.addEventListener('DOMContentLoaded', function () {
    console.log("Registering event click on button.");
    document.getElementById('use-webauthn').addEventListener('click', webauthnChallenge);
});