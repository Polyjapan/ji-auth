// Inspiration comes from https://codelabs.developers.google.com/codelabs/webauthn-reauth#2 but without the required libs o/

const registerCredentials = async () => {
    document.getElementById("register-error").hidden = true;

    if (!navigator.credentials) {
        document.getElementById("register-error-text").innerText = "Votre navigateur ne supporte pas l'utilisation de clés de sécurité.";
        document.getElementById("register-error").hidden = false;
        return;
    }

    if (!window.fetch) {
        document.getElementById("register-error-text").innerText = "Votre navigateur ne supporte pas l'utilisation des API Javascript récentes.";
        document.getElementById("register-error").hidden = false;
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

        let creds = null;
        try {
            creds = await navigator.credentials.create({publicKey: pk});
        } catch (e) {
            console.log("Error while using security key:");
            console.log(e);

            document.getElementById("register-error-text").innerText = "Votre clé de sécurité n'est pas reconnue. Peut être l'avez vous déjà enregistrée ?";
            document.getElementById("register-error").hidden = false;

            return;
        }
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

        window.location.reload();
    } else {
        document.getElementById("register-error-text").innerText = "Une erreur réseau s'est produite, merci de réessayer.";
        document.getElementById("register-error").hidden = false;
    }
}

window.registerCredentials = registerCredentials

document.getElementById("register-key-start").onclick = registerCredentials
