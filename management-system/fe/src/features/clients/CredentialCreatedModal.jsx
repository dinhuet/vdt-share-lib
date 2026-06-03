import Button from '../../components/Button';
import Modal from '../../components/Modal';

export default function CredentialCreatedModal({ credential, onClose }) {
  if (!credential) return null;

  return (
    <Modal
      title="Credential Generated"
      description="Copy these values now. They will not be shown again."
      onClose={onClose}
      footer={<Button onClick={onClose}>I have copied the credentials</Button>}
    >
      <p className="warning-text">Store the API key and signing secret securely before closing this dialog.</p>
      <dl className="credential-secrets">
        <dt>Client Code</dt>
        <dd><code>{credential.clientCode}</code></dd>
        <dt>Service</dt>
        <dd>{credential.microServiceName || credential.microServiceId}</dd>
        <dt>Key ID</dt>
        <dd><code>{credential.keyId}</code></dd>
        <dt>API Key</dt>
        <dd><code>{credential.apiKey}</code></dd>
        <dt>Signing Secret</dt>
        <dd><code>{credential.signingSecret}</code></dd>
        <dt>Algorithm</dt>
        <dd>{credential.algorithm}</dd>
      </dl>
    </Modal>
  );
}
