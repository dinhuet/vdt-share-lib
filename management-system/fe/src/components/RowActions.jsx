import { useEffect, useRef, useState } from 'react';

export default function RowActions({ actions, label = 'Actions' }) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const visibleActions = actions.filter(Boolean);

  function updatePosition() {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const menuWidth = 178;
    const menuHeight = Math.min(280, visibleActions.length * 38 + 12);
    const viewportPadding = 8;
    const left = Math.min(
      Math.max(viewportPadding, rect.right - menuWidth),
      window.innerWidth - menuWidth - viewportPadding,
    );
    const bottomTop = rect.bottom + viewportPadding;
    const top = bottomTop + menuHeight > window.innerHeight
      ? Math.max(viewportPadding, rect.top - menuHeight - viewportPadding)
      : bottomTop;
    setPosition({ top, left });
  }

  useEffect(() => {
    if (!open) return undefined;

    updatePosition();

    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    }

    function handleReposition() {
      updatePosition();
    }

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    window.addEventListener('resize', handleReposition);
    window.addEventListener('scroll', handleReposition, true);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('resize', handleReposition);
      window.removeEventListener('scroll', handleReposition, true);
    };
  }, [open, visibleActions.length]);

  if (visibleActions.length === 0) {
    return null;
  }

  return (
    <div className="row-actions action-menu" ref={rootRef}>
      <button
        ref={triggerRef}
        className="btn btn-secondary action-menu-trigger"
        type="button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => {
          updatePosition();
          setOpen((current) => !current);
        }}
      >
        <span aria-hidden="true">⋯</span>
      </button>
      {open ? (
        <div className="action-menu-popover" role="menu" style={{ top: position.top, left: position.left }}>
          {visibleActions.map((action) => (
            <button
              key={action.label}
              className={`action-menu-item ${action.danger ? 'is-danger' : ''}`}
              type="button"
              role="menuitem"
              disabled={action.disabled}
              onClick={() => {
                setOpen(false);
                action.onClick();
              }}
            >
              {action.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}
