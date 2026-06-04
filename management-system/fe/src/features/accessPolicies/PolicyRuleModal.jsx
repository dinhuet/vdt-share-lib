import Modal from '../../components/Modal';
import PolicyRuleForm from './PolicyRuleForm';

export default function PolicyRuleModal({ type, clients, saving, onClose, onCreate }) {
  const label = type === 'WHITE' ? 'Whitelist' : 'Blacklist';

  return (
    <Modal
      title={`Add ${label} Rule`}
      description={`Create a ${label.toLowerCase()} rule for the selected exposed API.`}
      onClose={onClose}
    >
      <PolicyRuleForm type={type} clients={clients} saving={saving} onCreate={onCreate} onCreated={onClose} />
    </Modal>
  );
}
