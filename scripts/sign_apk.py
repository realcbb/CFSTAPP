"""Sign a release APK using a private GitHub Actions secret; never print key data."""
import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile

EXPECTED_CERT = '625021ef0ac636921f17874ce9e7e5d84c4d00728bc233224694fb8b9127a433'

def main():
    raw = os.environ.get('CFST_SIGNING_JSON', '')
    if not raw:
        raise SystemExit('Missing repository secret CFST_SIGNING_JSON. No temporary signing key will be generated.')
    try:
        config = json.loads(raw)
        key_bytes = base64.b64decode(config['keystore_base64'], validate=True)
        password = config['password']
        alias = config['alias']
        if not isinstance(password, str) or '\n' in password or not password:
            raise ValueError()
        if not isinstance(alias, str) or not alias:
            raise ValueError()
    except (ValueError, KeyError, TypeError):
        raise SystemExit('Invalid CFST_SIGNING_JSON payload.') from None

    sdk = Path(os.environ['ANDROID_HOME'])
    signer = sdk / 'build-tools' / '34.0.0' / 'apksigner'
    unsigned = Path('app/build/outputs/apk/release/app-release-unsigned.apk')
    output = unsigned.with_name('CFSTAPP-release.apk')
    child_env = dict(os.environ)
    child_env.pop('CFST_SIGNING_JSON', None)
    with tempfile.TemporaryDirectory(prefix='cfst-signing-', dir=os.environ.get('RUNNER_TEMP')) as temp:
        folder = Path(temp)
        key = folder / 'release.p12'
        pw = folder / 'password.txt'
        key.write_bytes(key_bytes)
        pw.write_text(password, encoding='utf-8')
        key.chmod(0o600)
        pw.chmod(0o600)
        # Verify identity before signing. Only a public certificate is printed.
        cert = folder / 'cert.der'
        subprocess.run(['keytool', '-exportcert', '-keystore', str(key), '-storepass:file', str(pw),
            '-alias', alias, '-file', str(cert)], check=True, env=child_env,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        import hashlib
        if hashlib.sha256(cert.read_bytes()).hexdigest() != EXPECTED_CERT:
            raise SystemExit('Signing certificate does not match the fixed CFSTAPP certificate.')
        subprocess.run([str(signer), 'sign', '--ks', str(key), '--ks-key-alias', alias,
            '--ks-pass', 'file:' + str(pw), '--key-pass', 'file:' + str(pw),
            '--out', str(output), str(unsigned)], check=True, env=child_env)
        subprocess.run([str(signer), 'verify', '--verbose', '--print-certs', str(output)],
            check=True, env=child_env)

if __name__ == '__main__':
    main()
