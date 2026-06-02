export default function ToggleSwitch({ checked, disabled, onChange, label }) {
  return (
    <button
      aria-label={label || 'Toggle'}
      className={`toggle-switch ${checked ? 'is-on' : ''}`}
      disabled={disabled}
      type="button"
      onClick={() => onChange?.(!checked)}
    >
      <span />
    </button>
  );
}
