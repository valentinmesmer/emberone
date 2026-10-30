/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// UI/App State Store (Zustand)
// Handles client-side UI state: modals, selections, UI preferences, etc.

import { create } from 'zustand';

interface UIState {
  // Modal states
  modals: {
    [key: string]: boolean;
  };

  // Selected items
  // Last-selected organization for home/dashboard and sidebar switcher display (initialized to main org once).
  selectedOrganizationId: number | null;
  selectedUserId: number | null;
  selectedEventId: number | null;

  // UI preferences
  sidebarExpanded: boolean;
  theme: 'light' | 'dark' | 'system';

  // Mobile navigation drawer (opened from header hamburger or bottom-nav menu tab)
  navDrawerOpen: boolean;

  // Notification center panel (header bell)
  notificationPanelOpen: boolean;

  // Actions
  openModal: (modalKey: string) => void;
  closeModal: (modalKey: string) => void;
  toggleModal: (modalKey: string) => void;
  setSelectedOrganizationId: (id: number | null) => void;
  setSelectedUserId: (id: number | null) => void;
  setSelectedEventId: (id: number | null) => void;
  setSidebarExpanded: (expanded: boolean) => void;
  setTheme: (theme: 'light' | 'dark' | 'system') => void;
  setNavDrawerOpen: (open: boolean) => void;
  toggleNavDrawer: () => void;
  setNotificationPanelOpen: (open: boolean) => void;
  toggleNotificationPanel: () => void;
  reset: () => void;
}

const initialState = {
  modals: {},
  selectedOrganizationId: null,
  selectedUserId: null,
  selectedEventId: null,
  sidebarExpanded: true,
  theme: 'system' as const,
  navDrawerOpen: false,
  notificationPanelOpen: false,
};

export const useUIStore = create<UIState>((set) => ({
  ...initialState,

  openModal: (modalKey: string) =>
    set((state) => ({
      modals: { ...state.modals, [modalKey]: true },
    })),

  closeModal: (modalKey: string) =>
    set((state) => {
      const newModals = { ...state.modals };
      delete newModals[modalKey];
      return { modals: newModals };
    }),

  toggleModal: (modalKey: string) =>
    set((state) => ({
      modals: {
        ...state.modals,
        [modalKey]: !state.modals[modalKey],
      },
    })),

  setSelectedOrganizationId: (id: number | null) => set({ selectedOrganizationId: id }),

  setSelectedUserId: (id: number | null) => set({ selectedUserId: id }),

  setSelectedEventId: (id: number | null) => set({ selectedEventId: id }),

  setSidebarExpanded: (expanded: boolean) => set({ sidebarExpanded: expanded }),

  setTheme: (theme: 'light' | 'dark' | 'system') => set({ theme }),

  setNavDrawerOpen: (open: boolean) => set({ navDrawerOpen: open }),

  toggleNavDrawer: () => set((state) => ({ navDrawerOpen: !state.navDrawerOpen })),

  setNotificationPanelOpen: (open: boolean) => set({ notificationPanelOpen: open }),

  toggleNotificationPanel: () =>
    set((state) => ({ notificationPanelOpen: !state.notificationPanelOpen })),

  reset: () => set(initialState),
}));
