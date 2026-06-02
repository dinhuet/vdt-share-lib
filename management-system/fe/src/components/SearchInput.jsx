export default function SearchInput({ value, onChange, placeholder }) {
  return (
    <label className="search-input">
      <span>⌕</span>
      <input value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
