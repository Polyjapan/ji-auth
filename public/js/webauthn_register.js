// Inspiration comes from https://codelabs.developers.google.com/codelabs/webauthn-reauth#2 but without the required libs o/

const registerCredentials = async () => {
    if (!navigator.credentials) {
        $('#register-error-text').innerText = "Votre navigateur ne supporte pas l'utilisation de clés de sécurité.";
        $('#register-error').alert();
        return;
    }

    if (!window.fetch) {
        $('#register-error-text').innerText = "Votre navigateur ne supporte pas l'utilisation des API Javascript récentes.";
        $('#register-error').alert();
        return;
    }

    // Get token
    const csrfTokenNode = $("#csrf-token").children()[0]
    const csrfTokenName = csrfTokenNode.name
    const csrfTokenValue = csrfTokenNode.value

    // Fetch
    const response = await fetch("/webauthn/register/start")

    let contentType = response.headers.get("content-type");
    if (contentType && contentType.indexOf("text/json") !== -1) {
        const json = await response.json()

        const pk = json.pk;
        pk.challenge = base64url.decode(pk.challenge);
        pk.user.id = base64url.decode(pk.user.id);
        if (pk.excludeCredentials) {
            for (let cred of pk.excludeCredentials) {
                cred.id = base64url.decode(cred.id);
            }
        }

        const creds = await navigator.credentials.create({publicKey: pk});
        console.log(creds);
        const name = prompt("Donnez un surnom à cette méthode d'authentification forte");

        const credential = {};
        credential.id = creds.id;
        //credential.rawId = base64url.encode(creds.rawId);
        credential.type = creds.type;
        credential.clientExtensionResults = {};

        if (creds.response) {
            const clientDataJSON = base64url.encode(creds.response.clientDataJSON);
            const attestationObject = base64url.encode(creds.response.attestationObject);
            credential.response = {clientDataJSON, attestationObject};
        }

        const payload = {
            uid: json.uid,
            pk: credential,
            name: name
        }

        console.log(payload)
        console.log(creds)

        await fetch(`/webauthn/register/complete?${csrfTokenName}=${csrfTokenValue}`, {
            method: "POST",
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

    } else {
        $('#register-error-text').innerText = "Une erreur réseau s'est produite, merci de réessayer.";
        $('#register-error').alert();
    }
}

window.registerCredentials = registerCredentials

document.getElementById("register-key-start").onclick = registerCredentials
