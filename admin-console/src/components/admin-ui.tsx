import { useEffect, useRef, useState, type ReactNode } from 'react'

type ShellProps = {
  sidebar: ReactNode
  topbar: ReactNode
  children: ReactNode
  sidebarCollapsed?: boolean
}

export function AdminShell({ sidebar, topbar, children, sidebarCollapsed }: ShellProps) {
  return (
    <div className={sidebarCollapsed ? 'wp-shell wp-shell-collapsed' : 'wp-shell'}>
      <aside className="wp-sidebar">{sidebar}</aside>
      <div className="wp-main">
        <header className="wp-topbar">{topbar}</header>
        <main className="wp-content">{children}</main>
      </div>
    </div>
  )
}

type SectionHeaderProps = {
  title: string
  subtitle?: string
  right?: ReactNode
}

export function SectionHeader({ title, subtitle, right }: SectionHeaderProps) {
  return (
    <div className="wp-section-header">
      <div>
        <h2 className="wp-h2">{title}</h2>
        {subtitle ? <p className="wp-subtitle">{subtitle}</p> : null}
      </div>
      {right ? <div className="wp-section-header-actions">{right}</div> : null}
    </div>
  )
}

type StatCardProps = {
  label: string
  value: ReactNode
  hint?: ReactNode
}

export function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <article className="wp-stat-card">
      <p className="wp-stat-label">{label}</p>
      <p className="wp-stat-value">{value}</p>
      {hint ? <p className="wp-stat-hint">{hint}</p> : null}
    </article>
  )
}

type ModalProps = {
  title: string
  onClose: () => void
  children: ReactNode
}

export function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="wp-modal-backdrop" onClick={onClose}>
      <div className="wp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="wp-modal-header">
          <h3 className="wp-h3">{title}</h3>
          <button type="button" className="wp-btn wp-btn-ghost" onClick={onClose}>
            Fechar
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

type PaginationProps = {
  page: number
  totalPages: number
  onPrev: () => void
  onNext: () => void
  disablePrev: boolean
  disableNext: boolean
}

export function Pagination({
  page,
  totalPages,
  onPrev,
  onNext,
  disablePrev,
  disableNext,
}: PaginationProps) {
  return (
    <div className="wp-pagination">
      <button type="button" className="wp-btn wp-btn-ghost" disabled={disablePrev} onClick={onPrev}>
        Anterior
      </button>
      <span className="wp-muted">
        Página {page} / {totalPages}
      </span>
      <button type="button" className="wp-btn wp-btn-ghost" disabled={disableNext} onClick={onNext}>
        Seguinte
      </button>
    </div>
  )
}

type DropdownProps = {
  button: ReactNode
  children: ReactNode
  align?: 'start' | 'end'
}

export function Dropdown({ button, children, align = 'end' }: DropdownProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    function onDocClick(ev: MouseEvent) {
      const el = rootRef.current
      if (!el) return
      if (ev.target instanceof Node && el.contains(ev.target)) return
      setOpen(false)
    }
    function onEsc(ev: KeyboardEvent) {
      if (ev.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDocClick)
    document.addEventListener('keydown', onEsc)
    return () => {
      document.removeEventListener('mousedown', onDocClick)
      document.removeEventListener('keydown', onEsc)
    }
  }, [])

  return (
    <div className="wp-dropdown" ref={rootRef}>
      <div className="wp-dropdown-trigger" onClick={() => setOpen((v) => !v)}>
        {button}
      </div>
      {open ? (
        <div className={align === 'start' ? 'wp-dropdown-panel wp-dropdown-panel-start' : 'wp-dropdown-panel'}>
          {children}
        </div>
      ) : null}
    </div>
  )
}
