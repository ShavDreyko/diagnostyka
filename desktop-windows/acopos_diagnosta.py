"""ACOPOS Diagnosta — aplikacja desktopowa Windows Portable (.exe)."""

from __future__ import annotations

import json
import sys
import tkinter as tk
from pathlib import Path
from tkinter import ttk
from typing import Dict, List, Optional, Set

from acopos_engine import (
    CATEGORIES,
    COMMON_FAULT_CODES,
    SEVERITY_LABELS,
    AcoposError,
    build_diagnostic_report,
    load_catalog,
    search_catalog,
)

DARK_THEME = {
    "bg": "#0C1117",
    "surface": "#141C24",
    "surface_alt": "#1C2733",
    "border": "#2B3B4D",
    "fg": "#F1F5F9",
    "muted": "#94A3B8",
    "accent": "#FF7A00",
    "accent_fg": "#1A0D00",
    "cyan": "#38BDF8",
    "critical_fg": "#F87171",
    "critical_bg": "#3B1219",
    "error_fg": "#FB923C",
    "error_bg": "#3B1E0B",
    "warning_fg": "#FBBF24",
    "warning_bg": "#382807",
    "info_fg": "#38BDF8",
    "info_bg": "#0D2B3E",
    "success_fg": "#34D399",
    "success_bg": "#0C2E22",
}

LIGHT_THEME = {
    "bg": "#F3F5F8",
    "surface": "#FFFFFF",
    "surface_alt": "#E8EDF3",
    "border": "#CBD5E1",
    "fg": "#0F172A",
    "muted": "#475569",
    "accent": "#D65A00",
    "accent_fg": "#FFFFFF",
    "cyan": "#0284C7",
    "critical_fg": "#DC2626",
    "critical_bg": "#FEE2E2",
    "error_fg": "#EA580C",
    "error_bg": "#FFEDD5",
    "warning_fg": "#B45309",
    "warning_bg": "#FEF3C7",
    "info_fg": "#0369A1",
    "info_bg": "#E0F2FE",
    "success_fg": "#059669",
    "success_bg": "#D1FAE5",
}


def portable_storage_path() -> Path:
    """Zapisuje zakładki i historię obok pliku .exe (tryb w pełni przenośny / portable)."""
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent / "acopos_data.json"
    return Path(__file__).resolve().parent / "acopos_data.json"


class AcoposDiagnostaApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("ACOPOS Diagnosta — Windows Portable")
        self.geometry("1160x760")
        self.minsize(920, 620)

        self.catalog: List[AcoposError] = load_catalog()
        self.by_code: Dict[int, AcoposError] = {e.code: e for e in self.catalog}
        self.filtered: List[AcoposError] = list(self.catalog)
        self.selected_error: Optional[AcoposError] = None
        self.checked_steps: Dict[int, Set[int]] = {}
        self.bookmarks: Set[int] = set()
        self.recents: List[int] = []
        self.is_dark: bool = True
        self.only_bookmarks: bool = False

        self._load_portable_prefs()
        self._try_set_icon()

        self.search_var = tk.StringVar()
        self.category_var = tk.StringVar(value="Wszystkie kategorie")
        self.severity_var = tk.StringVar(value="Wszystkie poziomy")
        self.status_var = tk.StringVar(value=f"Załadowano {len(self.catalog)} kodów alarmów ACOPOS (offline)")

        self._build_ui()
        self._apply_theme()
        self._refresh_list()
        if 7222 in self.by_code:
            self._select_error(7222, record_recent=False)
        elif self.catalog:
            self._select_error(self.catalog[0].code, record_recent=False)

    def _try_set_icon(self) -> None:
        candidates = []
        if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
            candidates.append(Path(sys._MEIPASS) / "acopos_icon.ico")
        candidates.append(Path(__file__).resolve().parent / "acopos_icon.ico")
        for ico in candidates:
            if ico.is_file():
                try:
                    self.iconbitmap(default=str(ico))
                    break
                except Exception:
                    pass

    def _load_portable_prefs(self) -> None:
        path = portable_storage_path()
        if not path.is_file():
            return
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            self.bookmarks = {int(x) for x in data.get("bookmarks", []) if int(x) in self.by_code}
            self.recents = [int(x) for x in data.get("recents", []) if int(x) in self.by_code][:20]
            self.is_dark = bool(data.get("is_dark", True))
        except Exception:
            pass

    def _save_portable_prefs(self) -> None:
        path = portable_storage_path()
        try:
            payload = {
                "bookmarks": sorted(self.bookmarks),
                "recents": self.recents[:20],
                "is_dark": self.is_dark,
            }
            path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
        except Exception:
            pass

    def _palette(self) -> Dict[str, str]:
        return DARK_THEME if self.is_dark else LIGHT_THEME

    def _severity_colors(self, severity: str) -> tuple[str, str]:
        p = self._palette()
        return {
            "CRITICAL": (p["critical_fg"], p["critical_bg"]),
            "ERROR": (p["error_fg"], p["error_bg"]),
            "WARNING": (p["warning_fg"], p["warning_bg"]),
            "INFO": (p["info_fg"], p["info_bg"]),
        }.get(severity, (p["error_fg"], p["error_bg"]))

    def _build_ui(self) -> None:
        # Top header bar
        self.header_frame = tk.Frame(self, height=58, padx=16, pady=10)
        self.header_frame.pack(fill=tk.X)

        self.brand_label = tk.Label(
            self.header_frame,
            text="ACOPOS DIAGNOSTYKA",
            font=("Segoe UI Semibold", 15, "bold"),
        )
        self.brand_label.pack(side=tk.LEFT)

        self.sub_label = tk.Label(
            self.header_frame,
            text="  |  Serwonapędy B&R ACOPOS / Wittmann  •  Windows Portable",
            font=("Segoe UI", 10),
        )
        self.sub_label.pack(side=tk.LEFT, padx=(4, 0))

        self.theme_btn = tk.Button(
            self.header_frame,
            text="Tryb jasny",
            font=("Segoe UI Semibold", 9),
            relief=tk.FLAT,
            padx=12,
            pady=5,
            cursor="hand2",
            command=self._toggle_theme,
        )
        self.theme_btn.pack(side=tk.RIGHT)

        self.bookmarks_filter_btn = tk.Button(
            self.header_frame,
            text="★ Zapisane",
            font=("Segoe UI Semibold", 9),
            relief=tk.FLAT,
            padx=12,
            pady=5,
            cursor="hand2",
            command=self._toggle_bookmarks_filter,
        )
        self.bookmarks_filter_btn.pack(side=tk.RIGHT, padx=(0, 8))

        # Main split area
        self.body_frame = tk.Frame(self)
        self.body_frame.pack(fill=tk.BOTH, expand=True, padx=14, pady=(6, 10))

        # Left pane (search + filters + list)
        self.left_pane = tk.Frame(self.body_frame, width=410)
        self.left_pane.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 12))
        self.left_pane.pack_propagate(False)

        self.search_title = tk.Label(
            self.left_pane,
            text="Wpisz kod alarmu lub objaw:",
            font=("Segoe UI Semibold", 11),
            anchor="w",
        )
        self.search_title.pack(fill=tk.X, pady=(0, 6))

        search_row = tk.Frame(self.left_pane)
        search_row.pack(fill=tk.X, pady=(0, 8))
        self.search_row = search_row

        self.search_entry = tk.Entry(
            search_row,
            textvariable=self.search_var,
            font=("Consolas", 14, "bold"),
            relief=tk.FLAT,
            bd=6,
        )
        self.search_entry.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.search_entry.bind("<Return>", lambda _e: self._submit_search())
        self.search_var.trace_add("write", lambda *_: self._refresh_list())

        self.clear_btn = tk.Button(
            search_row,
            text="✕",
            font=("Segoe UI", 10, "bold"),
            relief=tk.FLAT,
            padx=10,
            cursor="hand2",
            command=lambda: self.search_var.set(""),
        )
        self.clear_btn.pack(side=tk.LEFT, padx=(6, 0))

        # Quick common faults chips
        self.quick_label = tk.Label(
            self.left_pane,
            text="Najczęstsze alarmy:",
            font=("Segoe UI", 9, "bold"),
            anchor="w",
        )
        self.quick_label.pack(fill=tk.X, pady=(2, 4))

        self.quick_frame = tk.Frame(self.left_pane)
        self.quick_frame.pack(fill=tk.X, pady=(0, 8))
        self.quick_buttons: List[tk.Button] = []
        for idx, code in enumerate(COMMON_FAULT_CODES[:8]):
            btn = tk.Button(
                self.quick_frame,
                text=str(code),
                font=("Consolas", 9, "bold"),
                relief=tk.FLAT,
                padx=8,
                pady=3,
                cursor="hand2",
                command=lambda c=code: self._jump_to_code(c),
            )
            btn.grid(row=idx // 4, column=idx % 4, padx=3, pady=3, sticky="ew")
            self.quick_frame.grid_columnconfigure(idx % 4, weight=1)
            self.quick_buttons.append(btn)

        # Category and severity comboboxes
        filter_row = tk.Frame(self.left_pane)
        filter_row.pack(fill=tk.X, pady=(0, 8))
        self.filter_row = filter_row

        cat_options = ["Wszystkie kategorie"] + [title for _, title, _ in CATEGORIES]
        self.cat_combo = ttk.Combobox(
            filter_row,
            textvariable=self.category_var,
            values=cat_options,
            state="readonly",
            font=("Segoe UI", 9),
        )
        self.cat_combo.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 6))
        self.cat_combo.bind("<<ComboboxSelected>>", lambda _e: self._refresh_list())

        sev_options = ["Wszystkie poziomy"] + list(SEVERITY_LABELS.values())
        self.sev_combo = ttk.Combobox(
            filter_row,
            textvariable=self.severity_var,
            values=sev_options,
            state="readonly",
            width=16,
            font=("Segoe UI", 9),
        )
        self.sev_combo.pack(side=tk.LEFT)
        self.sev_combo.bind("<<ComboboxSelected>>", lambda _e: self._refresh_list())

        self.count_label = tk.Label(self.left_pane, text="", font=("Segoe UI", 9), anchor="w")
        self.count_label.pack(fill=tk.X, pady=(0, 4))

        # Results listbox + scrollbar
        list_container = tk.Frame(self.left_pane)
        list_container.pack(fill=tk.BOTH, expand=True)
        self.list_container = list_container

        self.result_list = tk.Listbox(
            list_container,
            font=("Consolas", 10),
            activestyle="none",
            relief=tk.FLAT,
            bd=0,
            highlightthickness=1,
            selectmode=tk.SINGLE,
        )
        self.result_list.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.result_list.bind("<<ListboxSelect>>", self._on_list_select)

        list_scroll = ttk.Scrollbar(list_container, orient=tk.VERTICAL, command=self.result_list.yview)
        list_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.result_list.configure(yscrollcommand=list_scroll.set)

        # Right pane (scrollable diagnostic sheet)
        self.right_container = tk.Frame(self.body_frame)
        self.right_container.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.detail_canvas = tk.Canvas(self.right_container, highlightthickness=0, bd=0)
        self.detail_scroll = ttk.Scrollbar(
            self.right_container, orient=tk.VERTICAL, command=self.detail_canvas.yview
        )
        self.detail_inner = tk.Frame(self.detail_canvas, padx=18, pady=16)

        self.detail_window_id = self.detail_canvas.create_window(
            (0, 0), window=self.detail_inner, anchor="nw"
        )
        self.detail_canvas.configure(yscrollcommand=self.detail_scroll.set)

        self.detail_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.detail_scroll.pack(side=tk.RIGHT, fill=tk.Y)

        self.detail_inner.bind(
            "<Configure>",
            lambda _e: self.detail_canvas.configure(scrollregion=self.detail_canvas.bbox("all")),
        )
        self.detail_canvas.bind(
            "<Configure>",
            lambda e: self.detail_canvas.itemconfigure(self.detail_window_id, width=e.width),
        )
        self.detail_canvas.bind_all("<MouseWheel>", self._on_mousewheel)

        # Footer status bar
        self.status_bar = tk.Label(
            self,
            textvariable=self.status_var,
            font=("Segoe UI", 9),
            anchor="w",
            padx=16,
            pady=6,
        )
        self.status_bar.pack(fill=tk.X)

        self.search_entry.focus_set()

    def _on_mousewheel(self, event: tk.Event) -> None:
        if self.winfo_containing(event.x_root, event.y_root) in (self.result_list,):
            return
        delta = -1 * int(event.delta / 120) if event.delta else 0
        self.detail_canvas.yview_scroll(delta, "units")

    def _apply_theme(self) -> None:
        p = self._palette()
        self.configure(bg=p["bg"])
        self.header_frame.configure(bg=p["surface"])
        self.brand_label.configure(bg=p["surface"], fg=p["accent"])
        self.sub_label.configure(bg=p["surface"], fg=p["muted"])
        self.theme_btn.configure(
            bg=p["surface_alt"],
            fg=p["fg"],
            text="☀ Tryb jasny" if self.is_dark else "🌙 Tryb ciemny",
        )
        self.bookmarks_filter_btn.configure(
            bg=p["accent"] if self.only_bookmarks else p["surface_alt"],
            fg=p["accent_fg"] if self.only_bookmarks else p["fg"],
        )

        for frame in (self.body_frame, self.left_pane, self.search_row, self.quick_frame, self.filter_row, self.list_container):
            frame.configure(bg=p["bg"])

        self.search_title.configure(bg=p["bg"], fg=p["fg"])
        self.quick_label.configure(bg=p["bg"], fg=p["muted"])
        self.count_label.configure(bg=p["bg"], fg=p["muted"])
        self.search_entry.configure(
            bg=p["surface"],
            fg=p["fg"],
            insertbackground=p["accent"],
            highlightthickness=1,
            highlightcolor=p["accent"],
            highlightbackground=p["border"],
        )
        self.clear_btn.configure(bg=p["surface_alt"], fg=p["fg"])
        for btn in self.quick_buttons:
            btn.configure(bg=p["surface"], fg=p["accent"], activebackground=p["surface_alt"])

        self.result_list.configure(
            bg=p["surface"],
            fg=p["fg"],
            selectbackground=p["accent"],
            selectforeground=p["accent_fg"],
            highlightbackground=p["border"],
            highlightcolor=p["accent"],
        )
        self.right_container.configure(bg=p["surface"])
        self.detail_canvas.configure(bg=p["surface"])
        self.detail_inner.configure(bg=p["surface"])
        self.status_bar.configure(bg=p["surface_alt"], fg=p["muted"])

        if self.selected_error is not None:
            self._render_detail(self.selected_error)

    def _toggle_theme(self) -> None:
        self.is_dark = not self.is_dark
        self._save_portable_prefs()
        self._apply_theme()

    def _toggle_bookmarks_filter(self) -> None:
        self.only_bookmarks = not self.only_bookmarks
        self._apply_theme()
        self._refresh_list()

    def _selected_category_id(self) -> Optional[str]:
        val = self.category_var.get()
        for cid, title, _ in CATEGORIES:
            if title == val:
                return cid
        return None

    def _selected_severity_id(self) -> Optional[str]:
        val = self.severity_var.get()
        for sid, label in SEVERITY_LABELS.items():
            if label == val:
                return sid
        return None

    def _refresh_list(self) -> None:
        base = [e for e in self.catalog if e.code in self.bookmarks] if self.only_bookmarks else self.catalog
        self.filtered = search_catalog(
            base,
            self.search_var.get(),
            category_id=self._selected_category_id(),
            severity=self._selected_severity_id(),
        )
        self.result_list.delete(0, tk.END)
        for err in self.filtered:
            star = "★ " if err.code in self.bookmarks else ""
            title = err.title_pl if len(err.title_pl) <= 44 else err.title_pl[:43] + "…"
            self.result_list.insert(tk.END, f"{star}{err.code:>5}  {title}")

        mode_hint = " (tylko zapisane)" if self.only_bookmarks else ""
        self.count_label.configure(text=f"Wyniki: {len(self.filtered)} z {len(self.catalog)}{mode_hint}")

        # Auto-select exact numeric match while typing
        digits = self.search_var.get().strip().replace(".", "").replace(" ", "")
        if digits.isdigit():
            exact_code = int(digits)
            if exact_code in self.by_code:
                self._select_error(exact_code, record_recent=False)

    def _submit_search(self) -> None:
        if self.filtered:
            self._select_error(self.filtered[0].code, record_recent=True)

    def _jump_to_code(self, code: int) -> None:
        self.only_bookmarks = False
        self.category_var.set("Wszystkie kategorie")
        self.severity_var.set("Wszystkie poziomy")
        self.search_var.set(str(code))
        self._select_error(code, record_recent=True)

    def _on_list_select(self, _event: tk.Event) -> None:
        sel = self.result_list.curselection()
        if not sel:
            return
        idx = sel[0]
        if 0 <= idx < len(self.filtered):
            self._select_error(self.filtered[idx].code, record_recent=True)

    def _select_error(self, code: int, record_recent: bool = True) -> None:
        err = self.by_code.get(code)
        if err is None:
            return
        self.selected_error = err
        if record_recent:
            if code in self.recents:
                self.recents.remove(code)
            self.recents.insert(0, code)
            self.recents = self.recents[:20]
            self._save_portable_prefs()
        self._render_detail(err)
        self.detail_canvas.yview_moveto(0.0)

    def _toggle_bookmark(self, code: int) -> None:
        if code in self.bookmarks:
            self.bookmarks.remove(code)
            self.status_var.set(f"Usunięto kod {code} z zapisanych.")
        else:
            self.bookmarks.add(code)
            self.status_var.set(f"Zapisano kod {code} w zakładkach serwisowych.")
        self._save_portable_prefs()
        self._refresh_list()
        if self.selected_error and self.selected_error.code == code:
            self._render_detail(self.selected_error)

    def _copy_report(self, err: AcoposError) -> None:
        report = build_diagnostic_report(err, self.checked_steps.get(err.code, set()))
        self.clipboard_clear()
        self.clipboard_append(report)
        self.status_var.set(f"Skopiowano raport diagnostyczny dla alarmu {err.code} do schowka.")

    def _toggle_step(self, code: int, step_idx: int) -> None:
        steps = self.checked_steps.setdefault(code, set())
        if step_idx in steps:
            steps.remove(step_idx)
        else:
            steps.add(step_idx)
        if self.selected_error and self.selected_error.code == code:
            self._render_detail(self.selected_error)

    def _reset_steps(self, code: int) -> None:
        self.checked_steps.pop(code, None)
        if self.selected_error and self.selected_error.code == code:
            self._render_detail(self.selected_error)

    def _render_detail(self, err: AcoposError) -> None:
        for child in self.detail_inner.winfo_children():
            child.destroy()

        p = self._palette()
        sev_fg, sev_bg = self._severity_colors(err.severity)

        # Header row with code, severity badge, and action buttons
        top_row = tk.Frame(self.detail_inner, bg=p["surface"])
        top_row.pack(fill=tk.X, pady=(0, 10))

        code_badge = tk.Label(
            top_row,
            text=f" ALARM {err.code} ",
            font=("Consolas", 20, "bold"),
            bg=sev_bg,
            fg=sev_fg,
            padx=10,
            pady=4,
        )
        code_badge.pack(side=tk.LEFT)

        sev_badge = tk.Label(
            top_row,
            text=f" {err.severity_label.upper()} ",
            font=("Segoe UI Semibold", 10, "bold"),
            bg=sev_bg,
            fg=sev_fg,
            padx=8,
            pady=5,
        )
        sev_badge.pack(side=tk.LEFT, padx=(10, 0))

        cat_badge = tk.Label(
            top_row,
            text=f" {err.category_title} ",
            font=("Segoe UI", 9, "bold"),
            bg=p["surface_alt"],
            fg=p["fg"],
            padx=8,
            pady=5,
        )
        cat_badge.pack(side=tk.LEFT, padx=(8, 0))

        is_bm = err.code in self.bookmarks
        bm_btn = tk.Button(
            top_row,
            text="★ Zapisano" if is_bm else "☆ Zapisz",
            font=("Segoe UI Semibold", 9),
            bg=p["accent"] if is_bm else p["surface_alt"],
            fg=p["accent_fg"] if is_bm else p["fg"],
            relief=tk.FLAT,
            padx=10,
            pady=5,
            cursor="hand2",
            command=lambda c=err.code: self._toggle_bookmark(c),
        )
        bm_btn.pack(side=tk.RIGHT)

        copy_btn = tk.Button(
            top_row,
            text="📋 Kopiuj raport",
            font=("Segoe UI Semibold", 9),
            bg=p["surface_alt"],
            fg=p["fg"],
            relief=tk.FLAT,
            padx=10,
            pady=5,
            cursor="hand2",
            command=lambda e=err: self._copy_report(e),
        )
        copy_btn.pack(side=tk.RIGHT, padx=(0, 8))

        # Polish official title
        tk.Label(
            self.detail_inner,
            text=err.title_pl,
            font=("Segoe UI Semibold", 15, "bold"),
            bg=p["surface"],
            fg=p["fg"],
            justify=tk.LEFT,
            wraplength=650,
            anchor="w",
        ).pack(fill=tk.X, pady=(4, 6))

        if err.title_en:
            en_box = tk.Frame(self.detail_inner, bg=p["surface_alt"], padx=10, pady=6)
            en_box.pack(fill=tk.X, pady=(0, 8))
            tk.Label(
                en_box,
                text=f"Oryginał (EN): {err.title_en}",
                font=("Consolas", 9),
                bg=p["surface_alt"],
                fg=p["muted"],
                justify=tk.LEFT,
                wraplength=630,
                anchor="w",
            ).pack(fill=tk.X)

        if err.info_detail:
            info_box = tk.Frame(self.detail_inner, bg=p["info_bg"], padx=12, pady=8)
            info_box.pack(fill=tk.X, pady=(0, 10))
            tk.Label(
                info_box,
                text=f"Parametr diagnostyczny: {err.info_detail}",
                font=("Consolas", 9, "bold"),
                bg=p["info_bg"],
                fg=p["info_fg"],
                justify=tk.LEFT,
                wraplength=630,
                anchor="w",
            ).pack(fill=tk.X)
            if err.info_explanation_pl:
                tk.Label(
                    info_box,
                    text=err.info_explanation_pl,
                    font=("Segoe UI", 9),
                    bg=p["info_bg"],
                    fg=p["fg"],
                    justify=tk.LEFT,
                    wraplength=630,
                    anchor="w",
                ).pack(fill=tk.X, pady=(2, 0))

        if err.requires_hv_caution:
            hv_box = tk.Frame(self.detail_inner, bg=p["warning_bg"], padx=12, pady=8)
            hv_box.pack(fill=tk.X, pady=(0, 12))
            tk.Label(
                hv_box,
                text="⚠ UWAGA BHP: Przed pracami przy złączu X5, szynie DC i hamulcu odłącz zasilanie sieciowe i odczekaj na rozładowanie kondensatorów.",
                font=("Segoe UI Semibold", 9, "bold"),
                bg=p["warning_bg"],
                fg=p["warning_fg"],
                justify=tk.LEFT,
                wraplength=630,
                anchor="w",
            ).pack(fill=tk.X)

        self._add_section("Opis problemu", err.problem_description)
        self._add_section("Reakcja serwonapędu", err.drive_reaction)

        if err.possible_causes:
            causes_text = "\n".join(f"• {c}" for c in err.possible_causes)
            self._add_section("Najczęstsze przyczyny", causes_text)

        # Interactive repair checklist
        checked = self.checked_steps.get(err.code, set())
        done_count = sum(1 for i in range(len(err.repair_steps)) if i in checked)

        chk_header = tk.Frame(self.detail_inner, bg=p["surface"])
        chk_header.pack(fill=tk.X, pady=(12, 4))
        tk.Label(
            chk_header,
            text=f"Sugerowane kroki naprawcze ({done_count}/{len(err.repair_steps)})",
            font=("Segoe UI Semibold", 12, "bold"),
            bg=p["surface"],
            fg=p["accent"],
        ).pack(side=tk.LEFT)
        if done_count > 0:
            tk.Button(
                chk_header,
                text="Resetuj listę",
                font=("Segoe UI", 9),
                bg=p["surface_alt"],
                fg=p["fg"],
                relief=tk.FLAT,
                padx=8,
                pady=2,
                cursor="hand2",
                command=lambda c=err.code: self._reset_steps(c),
            ).pack(side=tk.RIGHT)

        for idx, step in enumerate(err.repair_steps):
            is_done = idx in checked
            row_bg = p["success_bg"] if is_done else p["surface_alt"]
            row_fg = p["success_fg"] if is_done else p["fg"]
            step_card = tk.Frame(self.detail_inner, bg=row_bg, padx=10, pady=8, cursor="hand2")
            step_card.pack(fill=tk.X, pady=3)

            mark = "✓" if is_done else f"{idx + 1}."
            lbl = tk.Label(
                step_card,
                text=f"{mark}  {step}",
                font=("Segoe UI", 10, "overstrike" if is_done else "normal"),
                bg=row_bg,
                fg=row_fg,
                justify=tk.LEFT,
                wraplength=630,
                anchor="w",
                cursor="hand2",
            )
            lbl.pack(fill=tk.X)
            step_card.bind("<Button-1>", lambda _e, c=err.code, i=idx: self._toggle_step(c, i))
            lbl.bind("<Button-1>", lambda _e, c=err.code, i=idx: self._toggle_step(c, i))

        # Related codes
        if err.related_codes:
            tk.Label(
                self.detail_inner,
                text="Powiązane kody alarmów",
                font=("Segoe UI Semibold", 11, "bold"),
                bg=p["surface"],
                fg=p["fg"],
                anchor="w",
            ).pack(fill=tk.X, pady=(14, 6))

            rel_row = tk.Frame(self.detail_inner, bg=p["surface"])
            rel_row.pack(fill=tk.X, pady=(0, 12))
            for rel_code in err.related_codes:
                rel_err = self.by_code.get(rel_code)
                if not rel_err:
                    continue
                short = rel_err.title_pl if len(rel_err.title_pl) <= 32 else rel_err.title_pl[:31] + "…"
                tk.Button(
                    rel_row,
                    text=f"{rel_code}: {short}",
                    font=("Consolas", 9),
                    bg=p["surface_alt"],
                    fg=p["cyan"],
                    relief=tk.FLAT,
                    padx=8,
                    pady=4,
                    cursor="hand2",
                    command=lambda c=rel_code: self._select_error(c, record_recent=True),
                ).pack(side=tk.LEFT, padx=(0, 6), pady=3)

    def _add_section(self, title: str, body: str) -> None:
        p = self._palette()
        box = tk.Frame(self.detail_inner, bg=p["surface_alt"], padx=12, pady=10)
        box.pack(fill=tk.X, pady=5)
        tk.Label(
            box,
            text=title,
            font=("Segoe UI Semibold", 10, "bold"),
            bg=p["surface_alt"],
            fg=p["accent"],
            anchor="w",
        ).pack(fill=tk.X, pady=(0, 4))
        tk.Label(
            box,
            text=body,
            font=("Segoe UI", 10),
            bg=p["surface_alt"],
            fg=p["fg"],
            justify=tk.LEFT,
            wraplength=630,
            anchor="w",
        ).pack(fill=tk.X)


def main() -> None:
    app = AcoposDiagnostaApp()
    app.mainloop()


if __name__ == "__main__":
    main()
