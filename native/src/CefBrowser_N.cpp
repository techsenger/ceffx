// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.
#include <iostream>
#include <cstdio>

#include "CefBrowser_N.h"
#include "include/base/cef_callback.h"
#include "include/cef_browser.h"
#include "include/cef_parser.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"
#include "browser_process_handler.h"
#include "client_handler.h"
#include "critical_wait.h"
#include "devtools_message_observer.h"
#include "int_callback.h"
#include "jni_util.h"
#include "life_span_handler.h"
#include "pdf_print_callback.h"
#include "render_handler.h"
#include "run_file_dialog_callback.h"
#include "string_visitor.h"
#include "temp_window.h"
#include "window_handler.h"
#if defined(OS_LINUX)
#define XK_3270  // for XK_3270_BackTab
#include <X11/XF86keysym.h>
#include <X11/keysym.h>
#include <memory>
#endif
#if defined(OS_MACOSX)
#include <Carbon/Carbon.h>
#include "util_mac.h"
#endif
#if defined(OS_WIN)
#include <memory>
#undef MOUSE_MOVED
#endif
namespace {

// ============================================================================
// Platform-specific KeyCode conversion functions
// ============================================================================

#if defined(OS_WIN)

int JavaFXKeyCodeToWindowsKeyCode(int javafx_key_code) {
  // Mapping of JavaFX KeyCode values to Windows virtual key codes
  switch (javafx_key_code) {
    case 0x08: return VK_BACK;           // BACK_SPACE
    case 0x09: return VK_TAB;            // TAB
    case 0x0C: return VK_CLEAR;          // CLEAR
    case 0x0A: return VK_RETURN;         // ENTER
    case 0x10: return VK_SHIFT;          // SHIFT
    case 0x11: return VK_CONTROL;        // CONTROL
    case 0x12: return VK_MENU;           // ALT
    case 0x13: return VK_PAUSE;          // PAUSE
    case 0x14: return VK_CAPITAL;        // CAPS
    case 0x1B: return VK_ESCAPE;         // ESCAPE
    case 0x20: return VK_SPACE;          // SPACE
    case 0x21: return VK_PRIOR;          // PAGE_UP
    case 0x22: return VK_NEXT;           // PAGE_DOWN
    case 0x23: return VK_END;            // END
    case 0x24: return VK_HOME;           // HOME
    case 0x25: return VK_LEFT;           // LEFT
    case 0x26: return VK_UP;             // UP
    case 0x27: return VK_RIGHT;          // RIGHT
    case 0x28: return VK_DOWN;           // DOWN
    case 0x2C: return VK_SNAPSHOT;       // PRINTSCREEN
    case 0x2D: return VK_INSERT;         // INSERT
    case 0x7F: return VK_DELETE;         // DELETE
    case 0x30: return '0';               // DIGIT0
    case 0x31: return '1';               // DIGIT1
    case 0x32: return '2';               // DIGIT2
    case 0x33: return '3';               // DIGIT3
    case 0x34: return '4';               // DIGIT4
    case 0x35: return '5';               // DIGIT5
    case 0x36: return '6';               // DIGIT6
    case 0x37: return '7';               // DIGIT7
    case 0x38: return '8';               // DIGIT8
    case 0x39: return '9';               // DIGIT9
    case 0x41: return 'A';               // A
    case 0x42: return 'B';               // B
    case 0x43: return 'C';               // C
    case 0x44: return 'D';               // D
    case 0x45: return 'E';               // E
    case 0x46: return 'F';               // F
    case 0x47: return 'G';               // G
    case 0x48: return 'H';               // H
    case 0x49: return 'I';               // I
    case 0x4A: return 'J';               // J
    case 0x4B: return 'K';               // K
    case 0x4C: return 'L';               // L
    case 0x4D: return 'M';               // M
    case 0x4E: return 'N';               // N
    case 0x4F: return 'O';               // O
    case 0x50: return 'P';               // P
    case 0x51: return 'Q';               // Q
    case 0x52: return 'R';               // R
    case 0x53: return 'S';               // S
    case 0x54: return 'T';               // T
    case 0x55: return 'U';               // U
    case 0x56: return 'V';               // V
    case 0x57: return 'W';               // W
    case 0x58: return 'X';               // X
    case 0x59: return 'Y';               // Y
    case 0x5A: return 'Z';               // Z
    case 0x5B: return VK_LWIN;           // OPEN_BRACKET/LWIN
    case 0x5C: return VK_OEM_5;          // BACK_SLASH
    case 0x5D: return VK_RWIN;           // CLOSE_BRACKET/RWIN
    case 0x60: return VK_NUMPAD0;        // NUMPAD0
    case 0x61: return VK_NUMPAD1;        // NUMPAD1
    case 0x62: return VK_NUMPAD2;        // NUMPAD2
    case 0x63: return VK_NUMPAD3;        // NUMPAD3
    case 0x64: return VK_NUMPAD4;        // NUMPAD4
    case 0x65: return VK_NUMPAD5;        // NUMPAD5
    case 0x66: return VK_NUMPAD6;        // NUMPAD6
    case 0x67: return VK_NUMPAD7;        // NUMPAD7
    case 0x68: return VK_NUMPAD8;        // NUMPAD8
    case 0x69: return VK_NUMPAD9;        // NUMPAD9
    case 0x6A: return VK_MULTIPLY;       // MULTIPLY
    case 0x6B: return VK_ADD;            // ADD
    case 0x6C: return VK_SEPARATOR;      // SEPARATOR
    case 0x6D: return VK_SUBTRACT;       // SUBTRACT
    case 0x6E: return VK_DECIMAL;        // DECIMAL
    case 0x6F: return VK_DIVIDE;         // DIVIDE
    case 0x70: return VK_F1;             // F1
    case 0x71: return VK_F2;             // F2
    case 0x72: return VK_F3;             // F3
    case 0x73: return VK_F4;             // F4
    case 0x74: return VK_F5;             // F5
    case 0x75: return VK_F6;             // F6
    case 0x76: return VK_F7;             // F7
    case 0x77: return VK_F8;             // F8
    case 0x78: return VK_F9;             // F9
    case 0x79: return VK_F10;            // F10
    case 0x7A: return VK_F11;            // F11
    case 0x7B: return VK_F12;            // F12
    case 0x90: return VK_NUMLOCK;        // NUM_LOCK
    case 0x91: return VK_SCROLL;         // SCROLL_LOCK
    case 0x9B: return VK_INSERT;         // INSERT (duplicate)
    case 0x9D: return VK_APPS;           // CONTEXT_MENU
    case 0x020C: return VK_LWIN;         // WINDOWS
    case 0xFF7E: return VK_MENU;         // ALT_GRAPH
    case 0x300: return VK_LWIN;          // COMMAND
    default: return 0;
  }
}

#endif  // defined(OS_WIN)

#if defined(OS_LINUX)

// From ui/events/keycodes/keyboard_codes_posix.h.
enum KeyboardCode {
  VKEY_BACK = 0x08,
  VKEY_TAB = 0x09,
  VKEY_BACKTAB = 0x0A,
  VKEY_CLEAR = 0x0C,
  VKEY_RETURN = 0x0D,
  VKEY_SHIFT = 0x10,
  VKEY_CONTROL = 0x11,
  VKEY_MENU = 0x12,
  VKEY_PAUSE = 0x13,
  VKEY_CAPITAL = 0x14,
  VKEY_KANA = 0x15,
  VKEY_HANGUL = 0x15,
  VKEY_JUNJA = 0x17,
  VKEY_FINAL = 0x18,
  VKEY_HANJA = 0x19,
  VKEY_KANJI = 0x19,
  VKEY_ESCAPE = 0x1B,
  VKEY_CONVERT = 0x1C,
  VKEY_NONCONVERT = 0x1D,
  VKEY_ACCEPT = 0x1E,
  VKEY_MODECHANGE = 0x1F,
  VKEY_SPACE = 0x20,
  VKEY_PRIOR = 0x21,
  VKEY_NEXT = 0x22,
  VKEY_END = 0x23,
  VKEY_HOME = 0x24,
  VKEY_LEFT = 0x25,
  VKEY_UP = 0x26,
  VKEY_RIGHT = 0x27,
  VKEY_DOWN = 0x28,
  VKEY_SELECT = 0x29,
  VKEY_PRINT = 0x2A,
  VKEY_EXECUTE = 0x2B,
  VKEY_SNAPSHOT = 0x2C,
  VKEY_INSERT = 0x2D,
  VKEY_DELETE = 0x2E,
  VKEY_HELP = 0x2F,
  VKEY_0 = 0x30,
  VKEY_1 = 0x31,
  VKEY_2 = 0x32,
  VKEY_3 = 0x33,
  VKEY_4 = 0x34,
  VKEY_5 = 0x35,
  VKEY_6 = 0x36,
  VKEY_7 = 0x37,
  VKEY_8 = 0x38,
  VKEY_9 = 0x39,
  VKEY_A = 0x41,
  VKEY_B = 0x42,
  VKEY_C = 0x43,
  VKEY_D = 0x44,
  VKEY_E = 0x45,
  VKEY_F = 0x46,
  VKEY_G = 0x47,
  VKEY_H = 0x48,
  VKEY_I = 0x49,
  VKEY_J = 0x4A,
  VKEY_K = 0x4B,
  VKEY_L = 0x4C,
  VKEY_M = 0x4D,
  VKEY_N = 0x4E,
  VKEY_O = 0x4F,
  VKEY_P = 0x50,
  VKEY_Q = 0x51,
  VKEY_R = 0x52,
  VKEY_S = 0x53,
  VKEY_T = 0x54,
  VKEY_U = 0x55,
  VKEY_V = 0x56,
  VKEY_W = 0x57,
  VKEY_X = 0x58,
  VKEY_Y = 0x59,
  VKEY_Z = 0x5A,
  VKEY_LWIN = 0x5B,
  VKEY_COMMAND = VKEY_LWIN,
  VKEY_RWIN = 0x5C,
  VKEY_APPS = 0x5D,
  VKEY_SLEEP = 0x5F,
  VKEY_NUMPAD0 = 0x60,
  VKEY_NUMPAD1 = 0x61,
  VKEY_NUMPAD2 = 0x62,
  VKEY_NUMPAD3 = 0x63,
  VKEY_NUMPAD4 = 0x64,
  VKEY_NUMPAD5 = 0x65,
  VKEY_NUMPAD6 = 0x66,
  VKEY_NUMPAD7 = 0x67,
  VKEY_NUMPAD8 = 0x68,
  VKEY_NUMPAD9 = 0x69,
  VKEY_MULTIPLY = 0x6A,
  VKEY_ADD = 0x6B,
  VKEY_SEPARATOR = 0x6C,
  VKEY_SUBTRACT = 0x6D,
  VKEY_DECIMAL = 0x6E,
  VKEY_DIVIDE = 0x6F,
  VKEY_F1 = 0x70,
  VKEY_F2 = 0x71,
  VKEY_F3 = 0x72,
  VKEY_F4 = 0x73,
  VKEY_F5 = 0x74,
  VKEY_F6 = 0x75,
  VKEY_F7 = 0x76,
  VKEY_F8 = 0x77,
  VKEY_F9 = 0x78,
  VKEY_F10 = 0x79,
  VKEY_F11 = 0x7A,
  VKEY_F12 = 0x7B,
  VKEY_F13 = 0x7C,
  VKEY_F14 = 0x7D,
  VKEY_F15 = 0x7E,
  VKEY_F16 = 0x7F,
  VKEY_F17 = 0x80,
  VKEY_F18 = 0x81,
  VKEY_F19 = 0x82,
  VKEY_F20 = 0x83,
  VKEY_F21 = 0x84,
  VKEY_F22 = 0x85,
  VKEY_F23 = 0x86,
  VKEY_F24 = 0x87,
  VKEY_NUMLOCK = 0x90,
  VKEY_SCROLL = 0x91,
  VKEY_LSHIFT = 0xA0,
  VKEY_RSHIFT = 0xA1,
  VKEY_LCONTROL = 0xA2,
  VKEY_RCONTROL = 0xA3,
  VKEY_LMENU = 0xA4,
  VKEY_RMENU = 0xA5,
  VKEY_BROWSER_BACK = 0xA6,
  VKEY_BROWSER_FORWARD = 0xA7,
  VKEY_BROWSER_REFRESH = 0xA8,
  VKEY_BROWSER_STOP = 0xA9,
  VKEY_BROWSER_SEARCH = 0xAA,
  VKEY_BROWSER_FAVORITES = 0xAB,
  VKEY_BROWSER_HOME = 0xAC,
  VKEY_VOLUME_MUTE = 0xAD,
  VKEY_VOLUME_DOWN = 0xAE,
  VKEY_VOLUME_UP = 0xAF,
  VKEY_MEDIA_NEXT_TRACK = 0xB0,
  VKEY_MEDIA_PREV_TRACK = 0xB1,
  VKEY_MEDIA_STOP = 0xB2,
  VKEY_MEDIA_PLAY_PAUSE = 0xB3,
  VKEY_MEDIA_LAUNCH_MAIL = 0xB4,
  VKEY_MEDIA_LAUNCH_MEDIA_SELECT = 0xB5,
  VKEY_MEDIA_LAUNCH_APP1 = 0xB6,
  VKEY_MEDIA_LAUNCH_APP2 = 0xB7,
  VKEY_OEM_1 = 0xBA,
  VKEY_OEM_PLUS = 0xBB,
  VKEY_OEM_COMMA = 0xBC,
  VKEY_OEM_MINUS = 0xBD,
  VKEY_OEM_PERIOD = 0xBE,
  VKEY_OEM_2 = 0xBF,
  VKEY_OEM_3 = 0xC0,
  VKEY_OEM_4 = 0xDB,
  VKEY_OEM_5 = 0xDC,
  VKEY_OEM_6 = 0xDD,
  VKEY_OEM_7 = 0xDE,
  VKEY_OEM_8 = 0xDF,
  VKEY_OEM_102 = 0xE2,
  VKEY_OEM_103 = 0xE3,
  VKEY_OEM_104 = 0xE4,
  VKEY_PROCESSKEY = 0xE5,
  VKEY_PACKET = 0xE7,
  VKEY_DBE_SBCSCHAR = 0xF3,
  VKEY_DBE_DBCSCHAR = 0xF4,
  VKEY_ATTN = 0xF6,
  VKEY_CRSEL = 0xF7,
  VKEY_EXSEL = 0xF8,
  VKEY_EREOF = 0xF9,
  VKEY_PLAY = 0xFA,
  VKEY_ZOOM = 0xFB,
  VKEY_NONAME = 0xFC,
  VKEY_PA1 = 0xFD,
  VKEY_OEM_CLEAR = 0xFE,
  VKEY_UNKNOWN = 0,
  VKEY_WLAN = 0x97,
  VKEY_POWER = 0x98,
  VKEY_BRIGHTNESS_DOWN = 0xD8,
  VKEY_BRIGHTNESS_UP = 0xD9,
  VKEY_KBD_BRIGHTNESS_DOWN = 0xDA,
  VKEY_KBD_BRIGHTNESS_UP = 0xE8,
  VKEY_ALTGR = 0xE1,
  VKEY_COMPOSE = 0xE6,
};

// From ui/events/keycodes/keyboard_code_conversion_x.cc.
KeyboardCode KeyboardCodeFromXKeysym(unsigned int keysym) {
  switch (keysym) {
    case XK_BackSpace:
      return VKEY_BACK;
    case XK_Delete:
    case XK_KP_Delete:
      return VKEY_DELETE;
    case XK_Tab:
    case XK_KP_Tab:
    case XK_ISO_Left_Tab:
    case XK_3270_BackTab:
      return VKEY_TAB;
    case XK_Linefeed:
    case XK_Return:
    case XK_KP_Enter:
    case XK_ISO_Enter:
      return VKEY_RETURN;
    case XK_Clear:
    case XK_KP_Begin:
      return VKEY_CLEAR;
    case XK_KP_Space:
    case XK_space:
      return VKEY_SPACE;
    case XK_Home:
    case XK_KP_Home:
      return VKEY_HOME;
    case XK_End:
    case XK_KP_End:
      return VKEY_END;
    case XK_Page_Up:
    case XK_KP_Page_Up:
      return VKEY_PRIOR;
    case XK_Page_Down:
    case XK_KP_Page_Down:
      return VKEY_NEXT;
    case XK_Left:
    case XK_KP_Left:
      return VKEY_LEFT;
    case XK_Right:
    case XK_KP_Right:
      return VKEY_RIGHT;
    case XK_Down:
    case XK_KP_Down:
      return VKEY_DOWN;
    case XK_Up:
    case XK_KP_Up:
      return VKEY_UP;
    case XK_Escape:
      return VKEY_ESCAPE;
    case XK_Kana_Lock:
    case XK_Kana_Shift:
      return VKEY_KANA;
    case XK_Hangul:
      return VKEY_HANGUL;
    case XK_Hangul_Hanja:
      return VKEY_HANJA;
    case XK_Kanji:
      return VKEY_KANJI;
    case XK_Henkan:
      return VKEY_CONVERT;
    case XK_Muhenkan:
      return VKEY_NONCONVERT;
    case XK_Zenkaku_Hankaku:
      return VKEY_DBE_DBCSCHAR;
    case XK_A:
    case XK_a:
      return VKEY_A;
    case XK_B:
    case XK_b:
      return VKEY_B;
    case XK_C:
    case XK_c:
      return VKEY_C;
    case XK_D:
    case XK_d:
      return VKEY_D;
    case XK_E:
    case XK_e:
      return VKEY_E;
    case XK_F:
    case XK_f:
      return VKEY_F;
    case XK_G:
    case XK_g:
      return VKEY_G;
    case XK_H:
    case XK_h:
      return VKEY_H;
    case XK_I:
    case XK_i:
      return VKEY_I;
    case XK_J:
    case XK_j:
      return VKEY_J;
    case XK_K:
    case XK_k:
      return VKEY_K;
    case XK_L:
    case XK_l:
      return VKEY_L;
    case XK_M:
    case XK_m:
      return VKEY_M;
    case XK_N:
    case XK_n:
      return VKEY_N;
    case XK_O:
    case XK_o:
      return VKEY_O;
    case XK_P:
    case XK_p:
      return VKEY_P;
    case XK_Q:
    case XK_q:
      return VKEY_Q;
    case XK_R:
    case XK_r:
      return VKEY_R;
    case XK_S:
    case XK_s:
      return VKEY_S;
    case XK_T:
    case XK_t:
      return VKEY_T;
    case XK_U:
    case XK_u:
      return VKEY_U;
    case XK_V:
    case XK_v:
      return VKEY_V;
    case XK_W:
    case XK_w:
      return VKEY_W;
    case XK_X:
    case XK_x:
      return VKEY_X;
    case XK_Y:
    case XK_y:
      return VKEY_Y;
    case XK_Z:
    case XK_z:
      return VKEY_Z;
    case XK_0:
    case XK_1:
    case XK_2:
    case XK_3:
    case XK_4:
    case XK_5:
    case XK_6:
    case XK_7:
    case XK_8:
    case XK_9:
      return static_cast<KeyboardCode>(VKEY_0 + (keysym - XK_0));
    case XK_parenright:
      return VKEY_0;
    case XK_exclam:
      return VKEY_1;
    case XK_at:
      return VKEY_2;
    case XK_numbersign:
      return VKEY_3;
    case XK_dollar:
      return VKEY_4;
    case XK_percent:
      return VKEY_5;
    case XK_asciicircum:
      return VKEY_6;
    case XK_ampersand:
      return VKEY_7;
    case XK_asterisk:
      return VKEY_8;
    case XK_parenleft:
      return VKEY_9;
    case XK_KP_0:
    case XK_KP_1:
    case XK_KP_2:
    case XK_KP_3:
    case XK_KP_4:
    case XK_KP_5:
    case XK_KP_6:
    case XK_KP_7:
    case XK_KP_8:
    case XK_KP_9:
      return static_cast<KeyboardCode>(VKEY_NUMPAD0 + (keysym - XK_KP_0));
    case XK_multiply:
    case XK_KP_Multiply:
      return VKEY_MULTIPLY;
    case XK_KP_Add:
      return VKEY_ADD;
    case XK_KP_Separator:
      return VKEY_SEPARATOR;
    case XK_KP_Subtract:
      return VKEY_SUBTRACT;
    case XK_KP_Decimal:
      return VKEY_DECIMAL;
    case XK_KP_Divide:
      return VKEY_DIVIDE;
    case XK_KP_Equal:
    case XK_equal:
    case XK_plus:
      return VKEY_OEM_PLUS;
    case XK_comma:
    case XK_less:
      return VKEY_OEM_COMMA;
    case XK_minus:
    case XK_underscore:
      return VKEY_OEM_MINUS;
    case XK_greater:
    case XK_period:
      return VKEY_OEM_PERIOD;
    case XK_colon:
    case XK_semicolon:
      return VKEY_OEM_1;
    case XK_question:
    case XK_slash:
      return VKEY_OEM_2;
    case XK_asciitilde:
    case XK_quoteleft:
      return VKEY_OEM_3;
    case XK_bracketleft:
    case XK_braceleft:
      return VKEY_OEM_4;
    case XK_backslash:
    case XK_bar:
      return VKEY_OEM_5;
    case XK_bracketright:
    case XK_braceright:
      return VKEY_OEM_6;
    case XK_quoteright:
    case XK_quotedbl:
      return VKEY_OEM_7;
    case XK_ISO_Level5_Shift:
      return VKEY_OEM_8;
    case XK_Shift_L:
    case XK_Shift_R:
      return VKEY_SHIFT;
    case XK_Control_L:
    case XK_Control_R:
      return VKEY_CONTROL;
    case XK_Meta_L:
    case XK_Meta_R:
    case XK_Alt_L:
    case XK_Alt_R:
      return VKEY_MENU;
    case XK_ISO_Level3_Shift:
      return VKEY_ALTGR;
    case XK_Multi_key:
      return VKEY_COMPOSE;
    case XK_Pause:
      return VKEY_PAUSE;
    case XK_Caps_Lock:
      return VKEY_CAPITAL;
    case XK_Num_Lock:
      return VKEY_NUMLOCK;
    case XK_Scroll_Lock:
      return VKEY_SCROLL;
    case XK_Select:
      return VKEY_SELECT;
    case XK_Print:
      return VKEY_PRINT;
    case XK_Execute:
      return VKEY_EXECUTE;
    case XK_Insert:
    case XK_KP_Insert:
      return VKEY_INSERT;
    case XK_Help:
      return VKEY_HELP;
    case XK_Super_L:
      return VKEY_LWIN;
    case XK_Super_R:
      return VKEY_RWIN;
    case XK_Menu:
      return VKEY_APPS;
    case XK_F1:
    case XK_F2:
    case XK_F3:
    case XK_F4:
    case XK_F5:
    case XK_F6:
    case XK_F7:
    case XK_F8:
    case XK_F9:
    case XK_F10:
    case XK_F11:
    case XK_F12:
    case XK_F13:
    case XK_F14:
    case XK_F15:
    case XK_F16:
    case XK_F17:
    case XK_F18:
    case XK_F19:
    case XK_F20:
    case XK_F21:
    case XK_F22:
    case XK_F23:
    case XK_F24:
      return static_cast<KeyboardCode>(VKEY_F1 + (keysym - XK_F1));
    case XK_KP_F1:
    case XK_KP_F2:
    case XK_KP_F3:
    case XK_KP_F4:
      return static_cast<KeyboardCode>(VKEY_F1 + (keysym - XK_KP_F1));
    case XK_guillemotleft:
    case XK_guillemotright:
    case XK_degree:
    case XK_ugrave:
    case XK_Ugrave:
    case XK_brokenbar:
      return VKEY_OEM_102;
    case XF86XK_Tools:
      return VKEY_F13;
    case XF86XK_Launch5:
      return VKEY_F14;
    case XF86XK_Launch6:
      return VKEY_F15;
    case XF86XK_Launch7:
      return VKEY_F16;
    case XF86XK_Launch8:
      return VKEY_F17;
    case XF86XK_Launch9:
      return VKEY_F18;
    case XF86XK_Refresh:
    case XF86XK_History:
    case XF86XK_OpenURL:
    case XF86XK_AddFavorite:
    case XF86XK_Go:
    case XF86XK_ZoomIn:
    case XF86XK_ZoomOut:
      return VKEY_UNKNOWN;
    case XF86XK_Back:
      return VKEY_BROWSER_BACK;
    case XF86XK_Forward:
      return VKEY_BROWSER_FORWARD;
    case XF86XK_Reload:
      return VKEY_BROWSER_REFRESH;
    case XF86XK_Stop:
      return VKEY_BROWSER_STOP;
    case XF86XK_Search:
      return VKEY_BROWSER_SEARCH;
    case XF86XK_Favorites:
      return VKEY_BROWSER_FAVORITES;
    case XF86XK_HomePage:
      return VKEY_BROWSER_HOME;
    case XF86XK_AudioMute:
      return VKEY_VOLUME_MUTE;
    case XF86XK_AudioLowerVolume:
      return VKEY_VOLUME_DOWN;
    case XF86XK_AudioRaiseVolume:
      return VKEY_VOLUME_UP;
    case XF86XK_AudioNext:
      return VKEY_MEDIA_NEXT_TRACK;
    case XF86XK_AudioPrev:
      return VKEY_MEDIA_PREV_TRACK;
    case XF86XK_AudioStop:
      return VKEY_MEDIA_STOP;
    case XF86XK_AudioPlay:
      return VKEY_MEDIA_PLAY_PAUSE;
    case XF86XK_Mail:
      return VKEY_MEDIA_LAUNCH_MAIL;
    case XF86XK_LaunchA:
      return VKEY_MEDIA_LAUNCH_APP1;
    case XF86XK_LaunchB:
    case XF86XK_Calculator:
      return VKEY_MEDIA_LAUNCH_APP2;
    case XF86XK_WLAN:
      return VKEY_WLAN;
    case XF86XK_PowerOff:
      return VKEY_POWER;
    case XF86XK_MonBrightnessDown:
      return VKEY_BRIGHTNESS_DOWN;
    case XF86XK_MonBrightnessUp:
      return VKEY_BRIGHTNESS_UP;
    case XF86XK_KbdBrightnessDown:
      return VKEY_KBD_BRIGHTNESS_DOWN;
    case XF86XK_KbdBrightnessUp:
      return VKEY_KBD_BRIGHTNESS_UP;
  }
  return VKEY_UNKNOWN;
}

KeyboardCode GetWindowsKeyCodeWithoutLocation(KeyboardCode key_code) {
  switch (key_code) {
    case VKEY_LCONTROL:
    case VKEY_RCONTROL:
      return VKEY_CONTROL;
    case VKEY_LSHIFT:
    case VKEY_RSHIFT:
      return VKEY_SHIFT;
    case VKEY_LMENU:
    case VKEY_RMENU:
      return VKEY_MENU;
    default:
      return key_code;
  }
}

int GetControlCharacter(KeyboardCode windows_key_code, bool shift) {
  if (windows_key_code >= VKEY_A && windows_key_code <= VKEY_Z) {
    return windows_key_code - VKEY_A + 1;
  }
  if (shift) {
    switch (windows_key_code) {
      case VKEY_2:
        return 0;
      case VKEY_6:
        return 0x1E;
      case VKEY_OEM_MINUS:
        return 0x1F;
      default:
        return 0;
    }
  } else {
    switch (windows_key_code) {
      case VKEY_OEM_4:
        return 0x1B;
      case VKEY_OEM_5:
        return 0x1C;
      case VKEY_OEM_6:
        return 0x1D;
      case VKEY_RETURN:
        return 0x0A;
      default:
        return 0;
    }
  }
}

unsigned int JavaFXKeyCodeToXKeysym(int javafx_key_code) {
  // Mapping of JavaFX KeyCode values to X11 keysyms
  switch (javafx_key_code) {
    case 0x08: return XK_BackSpace;
    case 0x09: return XK_Tab;
    case 0x0C: return XK_Clear;
    case 0x0A: return XK_Return;
    case 0x10: return XK_Shift_L;
    case 0x11: return XK_Control_L;
    case 0x12: return XK_Alt_L;
    case 0x13: return XK_Pause;
    case 0x14: return XK_Caps_Lock;
    case 0x1B: return XK_Escape;
    case 0x20: return XK_space;
    case 0x21: return XK_Page_Up;
    case 0x22: return XK_Page_Down;
    case 0x23: return XK_End;
    case 0x24: return XK_Home;
    case 0x25: return XK_Left;
    case 0x26: return XK_Up;
    case 0x27: return XK_Right;
    case 0x28: return XK_Down;
    case 0x2C: return XK_Print;
    case 0x2D: return XK_minus;
    case 0x2E: return XK_period;
    case 0x2F: return XK_slash;
    case 0x30: return XK_0;
    case 0x31: return XK_1;
    case 0x32: return XK_2;
    case 0x33: return XK_3;
    case 0x34: return XK_4;
    case 0x35: return XK_5;
    case 0x36: return XK_6;
    case 0x37: return XK_7;
    case 0x38: return XK_8;
    case 0x39: return XK_9;
    case 0x3B: return XK_semicolon;
    case 0x3D: return XK_equal;
    case 0x41: return XK_a;
    case 0x42: return XK_b;
    case 0x43: return XK_c;
    case 0x44: return XK_d;
    case 0x45: return XK_e;
    case 0x46: return XK_f;
    case 0x47: return XK_g;
    case 0x48: return XK_h;
    case 0x49: return XK_i;
    case 0x4A: return XK_j;
    case 0x4B: return XK_k;
    case 0x4C: return XK_l;
    case 0x4D: return XK_m;
    case 0x4E: return XK_n;
    case 0x4F: return XK_o;
    case 0x50: return XK_p;
    case 0x51: return XK_q;
    case 0x52: return XK_r;
    case 0x53: return XK_s;
    case 0x54: return XK_t;
    case 0x55: return XK_u;
    case 0x56: return XK_v;
    case 0x57: return XK_w;
    case 0x58: return XK_x;
    case 0x59: return XK_y;
    case 0x5A: return XK_z;
    case 0x5B: return XK_bracketleft;
    case 0x5C: return XK_backslash;
    case 0x5D: return XK_bracketright;
    case 0x60: return XK_KP_0;
    case 0x61: return XK_KP_1;
    case 0x62: return XK_KP_2;
    case 0x63: return XK_KP_3;
    case 0x64: return XK_KP_4;
    case 0x65: return XK_KP_5;
    case 0x66: return XK_KP_6;
    case 0x67: return XK_KP_7;
    case 0x68: return XK_KP_8;
    case 0x69: return XK_KP_9;
    case 0x6A: return XK_multiply;
    case 0x6B: return XK_KP_Add;
    case 0x6C: return XK_KP_Separator;
    case 0x6D: return XK_KP_Subtract;
    case 0x6E: return XK_KP_Decimal;
    case 0x6F: return XK_KP_Divide;
    case 0x7F: return XK_Delete;
    case 0x70: return XK_F1;
    case 0x71: return XK_F2;
    case 0x72: return XK_F3;
    case 0x73: return XK_F4;
    case 0x74: return XK_F5;
    case 0x75: return XK_F6;
    case 0x76: return XK_F7;
    case 0x77: return XK_F8;
    case 0x78: return XK_F9;
    case 0x79: return XK_F10;
    case 0x7A: return XK_F11;
    case 0x7B: return XK_F12;
    case 0x90: return XK_Num_Lock;
    case 0x91: return XK_Scroll_Lock;
    case 0x9B: return XK_Insert;
    case 0x9A: return XK_Print;
    case 0xC0: return XK_quoteleft;
    case 0xDE: return XK_quoteright;
    case 0xE0: return XK_KP_Up;
    case 0xE1: return XK_KP_Down;
    case 0xE2: return XK_KP_Left;
    case 0xE3: return XK_KP_Right;
    case 0xFF7E: return XK_ISO_Level3_Shift;
    case 0x20C: return XK_Super_L;
    case 0x300: return XK_Super_L;
    default: return XK_VoidSymbol;
  }
}

#endif  // defined(OS_LINUX)

#if defined(OS_MACOSX)

const char kShiftCharsForNumberKeys[] = ")!@#$%^&*(";

int JavaFXKeyCodeToMacKeyCode(int javafx_key_code) {
  switch (javafx_key_code) {
    case 0x08: return kVK_Delete;
    case 0x09: return kVK_Tab;
    case 0x0A: return kVK_Return;
    case 0x1B: return kVK_Escape;
    case 0x20: return kVK_Space;
    case 0x21: return kVK_PageUp;
    case 0x22: return kVK_PageDown;
    case 0x23: return kVK_End;
    case 0x24: return kVK_Home;
    case 0x25: return kVK_LeftArrow;
    case 0x26: return kVK_UpArrow;
    case 0x27: return kVK_RightArrow;
    case 0x28: return kVK_DownArrow;
    case 0x7F: return kVK_ForwardDelete;
    case 0x30: return kVK_ANSI_0;
    case 0x31: return kVK_ANSI_1;
    case 0x32: return kVK_ANSI_2;
    case 0x33: return kVK_ANSI_3;
    case 0x34: return kVK_ANSI_4;
    case 0x35: return kVK_ANSI_5;
    case 0x36: return kVK_ANSI_6;
    case 0x37: return kVK_ANSI_7;
    case 0x38: return kVK_ANSI_8;
    case 0x39: return kVK_ANSI_9;
    case 0x3B: return kVK_ANSI_Semicolon;
    case 0x3D: return kVK_ANSI_Equal;
    case 0x2C: return kVK_ANSI_Comma;
    case 0x2D: return kVK_ANSI_Minus;
    case 0x2E: return kVK_ANSI_Period;
    case 0x2F: return kVK_ANSI_Slash;
    case 0x41: return kVK_ANSI_A;
    case 0x42: return kVK_ANSI_B;
    case 0x43: return kVK_ANSI_C;
    case 0x44: return kVK_ANSI_D;
    case 0x45: return kVK_ANSI_E;
    case 0x46: return kVK_ANSI_F;
    case 0x47: return kVK_ANSI_G;
    case 0x48: return kVK_ANSI_H;
    case 0x49: return kVK_ANSI_I;
    case 0x4A: return kVK_ANSI_J;
    case 0x4B: return kVK_ANSI_K;
    case 0x4C: return kVK_ANSI_L;
    case 0x4D: return kVK_ANSI_M;
    case 0x4E: return kVK_ANSI_N;
    case 0x4F: return kVK_ANSI_O;
    case 0x50: return kVK_ANSI_P;
    case 0x51: return kVK_ANSI_Q;
    case 0x52: return kVK_ANSI_R;
    case 0x53: return kVK_ANSI_S;
    case 0x54: return kVK_ANSI_T;
    case 0x55: return kVK_ANSI_U;
    case 0x56: return kVK_ANSI_V;
    case 0x57: return kVK_ANSI_W;
    case 0x58: return kVK_ANSI_X;
    case 0x59: return kVK_ANSI_Y;
    case 0x5A: return kVK_ANSI_Z;
    case 0x5B: return kVK_ANSI_LeftBracket;
    case 0x5C: return kVK_ANSI_Backslash;
    case 0x5D: return kVK_ANSI_RightBracket;
    case 0xC0: return kVK_ANSI_Grave;
    case 0xDE: return kVK_ANSI_Quote;
    case 0x70: return kVK_F1;
    case 0x71: return kVK_F2;
    case 0x72: return kVK_F3;
    case 0x73: return kVK_F4;
    case 0x74: return kVK_F5;
    case 0x75: return kVK_F6;
    case 0x76: return kVK_F7;
    case 0x77: return kVK_F8;
    case 0x78: return kVK_F9;
    case 0x79: return kVK_F10;
    case 0x7A: return kVK_F11;
    case 0x7B: return kVK_F12;
    case 0x10: return kVK_Shift;
    case 0x11: return kVK_Control;
    case 0x12: return kVK_Option;
    case 0x9D: return kVK_Command;
    case 0x300: return kVK_Command;
    default: return -1;
  }
}

char GetMacShiftCharacter(int mac_key_code) {
  switch (mac_key_code) {
    case kVK_ANSI_Grave: return '~';
    case kVK_ANSI_1: return '!';
    case kVK_ANSI_2: return '@';
    case kVK_ANSI_3: return '#';
    case kVK_ANSI_4: return '$';
    case kVK_ANSI_5: return '%';
    case kVK_ANSI_6: return '^';
    case kVK_ANSI_7: return '&';
    case kVK_ANSI_8: return '*';
    case kVK_ANSI_9: return '(';
    case kVK_ANSI_0: return ')';
    case kVK_ANSI_Minus: return '_';
    case kVK_ANSI_Equal: return '+';
    case kVK_ANSI_LeftBracket: return '{';
    case kVK_ANSI_RightBracket: return '}';
    case kVK_ANSI_Backslash: return '|';
    case kVK_ANSI_Semicolon: return ':';
    case kVK_ANSI_Quote: return '"';
    case kVK_ANSI_Comma: return '<';
    case kVK_ANSI_Period: return '>';
    case kVK_ANSI_Slash: return '?';
    default: return 0;
  }
}

char GetMacControlCharacter(int mac_key_code) {
  switch (mac_key_code) {
    case kVK_ANSI_LeftBracket: return 27;
    case kVK_ANSI_Backslash: return 28;
    case kVK_ANSI_RightBracket: return 29;
    default: return 0;
  }
}

#endif  // defined(OS_MACOSX)

///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////

int GetCefModifiers(JNIEnv* env, jclass cls, int modifiers) {
  JNI_STATIC_DEFINE_INT_RV(env, cls, ALT_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON1_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON2_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON3_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, CTRL_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, META_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, SHIFT_DOWN_MASK, 0);
  int cef_modifiers = 0;
  if (modifiers & JNI_STATIC(ALT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_ALT_DOWN;
  if (modifiers & JNI_STATIC(BUTTON1_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_LEFT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON2_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_MIDDLE_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON3_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_RIGHT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(CTRL_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_CONTROL_DOWN;
  if (modifiers & JNI_STATIC(META_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_COMMAND_DOWN;
  if (modifiers & JNI_STATIC(SHIFT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_SHIFT_DOWN;
  return cef_modifiers;
}

int GetCefModifiersFromJavaFXInput(JNIEnv* env, jobject event) {
  int cef_modifiers = 0;

  jclass eventClass = env->GetObjectClass(event);
  if (!eventClass)
    return 0;

  jmethodID isCtrlDownMethod =
      env->GetMethodID(eventClass, "isControlDown", "()Z");
  jmethodID isShiftDownMethod =
      env->GetMethodID(eventClass, "isShiftDown", "()Z");
  jmethodID isAltDownMethod =
      env->GetMethodID(eventClass, "isAltDown", "()Z");
  jmethodID isMetaDownMethod =
      env->GetMethodID(eventClass, "isMetaDown", "()Z");

  if (isCtrlDownMethod &&
      env->CallBooleanMethod(event, isCtrlDownMethod))
    cef_modifiers |= EVENTFLAG_CONTROL_DOWN;

  if (isShiftDownMethod &&
      env->CallBooleanMethod(event, isShiftDownMethod))
    cef_modifiers |= EVENTFLAG_SHIFT_DOWN;

  if (isAltDownMethod &&
      env->CallBooleanMethod(event, isAltDownMethod))
    cef_modifiers |= EVENTFLAG_ALT_DOWN;

  if (isMetaDownMethod &&
      env->CallBooleanMethod(event, isMetaDownMethod))
    cef_modifiers |= EVENTFLAG_COMMAND_DOWN;

  return cef_modifiers;
}

int GetCefModifiersFromJavaFXMouse(JNIEnv* env, jobject event) {
  int cef_modifiers = GetCefModifiersFromJavaFXInput(env, event);

  jclass eventClass = env->GetObjectClass(event);
  if (!eventClass)
    return cef_modifiers;

  jmethodID isPrimaryDownMethod =
      env->GetMethodID(eventClass, "isPrimaryButtonDown", "()Z");
  jmethodID isSecondaryDownMethod =
      env->GetMethodID(eventClass, "isSecondaryButtonDown", "()Z");
  jmethodID isMiddleDownMethod =
      env->GetMethodID(eventClass, "isMiddleButtonDown", "()Z");

  if (isPrimaryDownMethod &&
      env->CallBooleanMethod(event, isPrimaryDownMethod))
    cef_modifiers |= EVENTFLAG_LEFT_MOUSE_BUTTON;

  if (isSecondaryDownMethod &&
      env->CallBooleanMethod(event, isSecondaryDownMethod))
    cef_modifiers |= EVENTFLAG_RIGHT_MOUSE_BUTTON;

  if (isMiddleDownMethod &&
      env->CallBooleanMethod(event, isMiddleDownMethod))
    cef_modifiers |= EVENTFLAG_MIDDLE_MOUSE_BUTTON;

  return cef_modifiers;
}

struct JNIObjectsForCreate {
 public:
  ScopedJNIObjectGlobal jbrowser;
  ScopedJNIObjectGlobal jparentBrowser;
  ScopedJNIObjectGlobal jclientHandler;
  ScopedJNIObjectGlobal url;
  ScopedJNIObjectGlobal jcontext;
  ScopedJNIObjectGlobal jinspectAt;
  ScopedJNIObjectGlobal jbrowserSettings;
  JNIObjectsForCreate(JNIEnv* env,
                      jobject _jbrowser,
                      jobject _jparentBrowser,
                      jobject _jclientHandler,
                      jstring _url,
                      jobject _jcontext,
                      jobject _jinspectAt,
                      jobject _browserSettings)
      :
        jbrowser(env, _jbrowser),
        jparentBrowser(env, _jparentBrowser),
        jclientHandler(env, _jclientHandler),
        url(env, _url),
        jcontext(env, _jcontext),
        jinspectAt(env, _jinspectAt),
        jbrowserSettings(env, _browserSettings) {}
};

void create(std::shared_ptr<JNIObjectsForCreate> objs,
            jlong windowHandle,
            jboolean osr,
            jboolean transparent) {
  ScopedJNIEnv env;
  CefRefPtr<ClientHandler> clientHandler = GetCefFromJNIObject<ClientHandler>(
      env, objs->jclientHandler, "CefClientHandler");
  if (!clientHandler.get())
    return;
  CefRefPtr<LifeSpanHandler> lifeSpanHandler =
      (LifeSpanHandler*)clientHandler->GetLifeSpanHandler().get();
  if (!lifeSpanHandler.get())
    return;
  CefRefPtr<CefBrowser> parentBrowser =
      GetCefFromJNIObject<CefBrowser>(env, objs->jparentBrowser, "CefBrowser");
  CefWindowInfo windowInfo;
  CefBrowserSettings settings;
  // If parentBrowser is set, we want to show the DEV-Tools for that browser.
  // Since that cannot be an Alloy-style window, it cannot be integrated into
  // Java UI but must be opened as a pop-up.
  if (parentBrowser.get() != nullptr) {
    CefPoint inspectAt;
    if (objs->jinspectAt != nullptr) {
      int x, y;
      GetJNIPoint2D(env, objs->jinspectAt, &x, &y);
      inspectAt.Set(x, y);
    }
    parentBrowser->GetHost()->ShowDevTools(windowInfo, clientHandler.get(),
                                           settings, inspectAt);
    JNI_CALL_VOID_METHOD(env, objs->jbrowser, "notifyBrowserCreated", "()V");
    return;
  }
  if (osr == JNI_FALSE) {
    CefRect rect;
    CefRefPtr<WindowHandler> windowHandler =
        (WindowHandler*)clientHandler->GetWindowHandler().get();
    if (windowHandler.get()) {
      windowHandler->GetRect(objs->jbrowser, rect);
    }
#if defined(OS_WIN)
    CefWindowHandle parent = TempWindow::GetWindowHandle();
    if (windowHandle != 0) {
      parent = (CefWindowHandle)windowHandle;
    } else {
      // Do not activate hidden browser windows on creation.
      windowInfo.ex_style |= WS_EX_NOACTIVATE;
    }
    windowInfo.SetAsChild(parent, rect);
#elif defined(OS_MACOSX)
    NSWindow* parent = nullptr;
    if (windowHandle != 0) {
      parent = (NSWindow*)windowHandle;
    } else {
      parent = TempWindow::GetWindow();
    }
    CefWindowHandle browserContentView =
        util_mac::CreateBrowserContentView(parent, rect);
    windowInfo.SetAsChild(browserContentView, rect);
#elif defined(OS_LINUX)
    CefWindowHandle parent = TempWindow::GetWindowHandle();
    if (windowHandle != 0) {
      parent = (CefWindowHandle)windowHandle;
    }
    windowInfo.SetAsChild(parent, rect);
#endif
  } else {
    windowInfo.SetAsWindowless((CefWindowHandle)windowHandle);
  }
  if (transparent == JNI_FALSE) {
    // Specify an opaque background color (white) to disable transparency.
    settings.background_color = CefColorSetARGB(255, 255, 255, 255);
  }
  ScopedJNIClass cefBrowserSettings(env, "com/techsenger/ceffx/core/CefBrowserSettings");
  if (cefBrowserSettings != nullptr &&
      objs->jbrowserSettings != nullptr) {  // Dev-tools settings are null
    GetJNIFieldInt(env, cefBrowserSettings, objs->jbrowserSettings,
                   "windowless_frame_rate", &settings.windowless_frame_rate);
  }
  CefRefPtr<CefBrowser> browserObj;
  CefString strUrl = GetJNIString(env, static_cast<jstring>(objs->url.get()));
  CefRefPtr<CefRequestContext> context = GetCefFromJNIObject<CefRequestContext>(
      env, objs->jcontext, "CefRequestContext");
  // Add a global ref that will be released in LifeSpanHandler::OnAfterCreated.
  jobject globalRef = env->NewGlobalRef(objs->jbrowser);
  lifeSpanHandler->registerJBrowser(globalRef);
  CefRefPtr<CefDictionaryValue> extra_info;
  auto router_configs = BrowserProcessHandler::GetMessageRouterConfigs();
  if (router_configs) {
    // Send the message router config to CefHelperApp::OnBrowserCreated.
    extra_info = CefDictionaryValue::Create();
    extra_info->SetList("router_configs", router_configs);
  }
  // CEFFX requires Alloy runtime style for "normal" browsers in order for them
  // to be integratable into Java UI.
  windowInfo.runtime_style = CEF_RUNTIME_STYLE_ALLOY;
  bool result = CefBrowserHost::CreateBrowser(
      windowInfo, clientHandler.get(), strUrl, settings, extra_info, context);
  if (!result) {
    lifeSpanHandler->unregisterJBrowser(globalRef);
    env->DeleteGlobalRef(globalRef);
    return;
  }
  JNI_CALL_VOID_METHOD(env, objs->jbrowser, "notifyBrowserCreated", "()V");
}

void getZoomLevel(CefRefPtr<CefBrowserHost> host,
                  CriticalWait* waitCond,
                  double* result) {
  if (waitCond && result) {
    waitCond->lock()->Lock();
    *result = host->GetZoomLevel();
    waitCond->WakeUp();
    waitCond->lock()->Unlock();
  }
}
void executeDevToolsMethod(CefRefPtr<CefBrowserHost> host,
                           const CefString& method,
                           const CefString& parametersAsJson,
                           CefRefPtr<IntCallback> callback) {
  CefRefPtr<CefDictionaryValue> parameters = nullptr;
  if (!parametersAsJson.empty()) {
    CefRefPtr<CefValue> value = CefParseJSON(
        parametersAsJson, cef_json_parser_options_t::JSON_PARSER_RFC);
    if (!value || value->GetType() != VTYPE_DICTIONARY) {
      callback->onComplete(0);
      return;
    }
    parameters = value->GetDictionary();
  }
  callback->onComplete(host->ExecuteDevToolsMethod(0, method, parameters));
}
void OnAfterParentChanged(CefRefPtr<CefBrowser> browser) {
  if (!CefCurrentlyOn(TID_UI)) {
    CefPostTask(TID_UI, base::BindOnce(&OnAfterParentChanged, browser));
    return;
  }
  if (browser->GetHost()->GetClient()) {
    CefRefPtr<LifeSpanHandler> lifeSpanHandler =
        (LifeSpanHandler*)browser->GetHost()
            ->GetClient()
            ->GetLifeSpanHandler()
            .get();
    if (lifeSpanHandler) {
      lifeSpanHandler->OnAfterParentChanged(browser);
    }
  }
}
CefPdfPrintSettings GetJNIPdfPrintSettings(JNIEnv* env, jobject obj) {
  CefString tmp;
  CefPdfPrintSettings settings;
  if (!obj)
    return settings;
  ScopedJNIClass cls(env, "com/techsenger/ceffx/core/misc/CefPdfPrintSettings");
  if (!cls)
    return settings;
  GetJNIFieldBoolean(env, cls, obj, "landscape", &settings.landscape);
  GetJNIFieldBoolean(env, cls, obj, "print_background",
                     &settings.print_background);
  GetJNIFieldDouble(env, cls, obj, "scale", &settings.scale);
  GetJNIFieldDouble(env, cls, obj, "paper_width", &settings.paper_width);
  GetJNIFieldDouble(env, cls, obj, "paper_height", &settings.paper_height);
  GetJNIFieldBoolean(env, cls, obj, "prefer_css_page_size",
                     &settings.prefer_css_page_size);
  jobject obj_margin_type = nullptr;
  if (GetJNIFieldObject(env, cls, obj, "margin_type", &obj_margin_type,
                        "Lcom/techsenger/ceffx/core/misc/CefPdfPrintSettings$MarginType;")) {
    ScopedJNIObjectLocal margin_type(env, obj_margin_type);
    if (IsJNIEnumValue(env, margin_type,
                       "com/techsenger/ceffx/core/misc/CefPdfPrintSettings$MarginType",
                       "DEFAULT")) {
      settings.margin_type = PDF_PRINT_MARGIN_DEFAULT;
    } else if (IsJNIEnumValue(env, margin_type,
                              "com/techsenger/ceffx/core/misc/CefPdfPrintSettings$MarginType",
                              "NONE")) {
      settings.margin_type = PDF_PRINT_MARGIN_NONE;
    } else if (IsJNIEnumValue(env, margin_type,
                              "com/techsenger/ceffx/core/misc/CefPdfPrintSettings$MarginType",
                              "CUSTOM")) {
      settings.margin_type = PDF_PRINT_MARGIN_CUSTOM;
    }
  }
  GetJNIFieldDouble(env, cls, obj, "margin_top", &settings.margin_top);
  GetJNIFieldDouble(env, cls, obj, "margin_bottom", &settings.margin_bottom);
  GetJNIFieldDouble(env, cls, obj, "margin_right", &settings.margin_right);
  GetJNIFieldDouble(env, cls, obj, "margin_left", &settings.margin_left);
  if (GetJNIFieldString(env, cls, obj, "page_ranges", &tmp) && !tmp.empty()) {
    CefString(&settings.page_ranges) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "display_header_footer",
                     &settings.display_header_footer);
  if (GetJNIFieldString(env, cls, obj, "header_template", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.header_template) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "footer_template", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.footer_template) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "generate_tagged_pdf",
                     &settings.generate_tagged_pdf);
  GetJNIFieldBoolean(env, cls, obj, "generate_document_outline",
                     &settings.generate_document_outline);
  return settings;
}
// JNI CefRegistration object.
class ScopedJNIRegistration : public ScopedJNIObject<CefRegistration> {
 public:
  ScopedJNIRegistration(JNIEnv* env, CefRefPtr<CefRegistration> obj)
      : ScopedJNIObject<CefRegistration>(env,
                                         obj,
                                         "com/techsenger/ceffx/core/browser/CefRegistration_N",
                                         "CefRegistration") {}
};
}  // namespace
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1CreateBrowser(JNIEnv* env,
                                                    jobject jbrowser,
                                                    jobject jclientHandler,
                                                    jlong windowHandle,
                                                    jstring url,
                                                    jboolean osr,
                                                    jboolean transparent,
                                                    jobject jcontext,
                                                    jobject browserSettings) {
  std::shared_ptr<JNIObjectsForCreate> objs(
      new JNIObjectsForCreate(env, jbrowser, nullptr, jclientHandler, url,
                              jcontext, nullptr, browserSettings));
  if (CefCurrentlyOn(TID_UI)) {
    create(objs, windowHandle, osr, transparent);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(&create, objs, windowHandle, osr, transparent));
  }
  return JNI_FALSE;  // set asynchronously
}

JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1CreateDevTools(
    JNIEnv* env,
    jobject jbrowser,
    jobject jparent,
    jobject jclientHandler,
    jlong windowHandle,
    jboolean osr,
    jboolean transparent,
    jobject inspect) {

  std::shared_ptr<JNIObjectsForCreate> objs(
      new JNIObjectsForCreate(env,
                              jbrowser,
                              jparent,
                              jclientHandler,
                              nullptr,
                              nullptr,
                              inspect,
                              nullptr));

  if (CefCurrentlyOn(TID_UI)) {
    create(objs, windowHandle, osr, transparent);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(&create, objs, windowHandle, osr, transparent));
  }

  return JNI_FALSE;  // async operation
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1ExecuteDevToolsMethod(
    JNIEnv* env,
    jobject jbrowser,
    jstring method,
    jstring parametersAsJson,
    jobject jcallback) {
  CefRefPtr<IntCallback> callback = new IntCallback(env, jcallback);
  CefRefPtr<CefBrowser> browser = GetJNIBrowser(env, jbrowser);
  if (!browser.get()) {
    callback->onComplete(0);
    return;
  }
  CefString strMethod = GetJNIString(env, method);
  CefString strParametersAsJson = GetJNIString(env, parametersAsJson);
  if (CefCurrentlyOn(TID_UI)) {
    executeDevToolsMethod(browser->GetHost(), strMethod, strParametersAsJson,
                          callback);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(executeDevToolsMethod, browser->GetHost(),
                               strMethod, strParametersAsJson, callback));
  }
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1AddDevToolsMessageObserver(
    JNIEnv* env,
    jobject jbrowser,
    jobject jobserver) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, jbrowser, NULL);
  CefRefPtr<DevToolsMessageObserver> observer =
      new DevToolsMessageObserver(env, jobserver);
  CefRefPtr<CefRegistration> registration =
      browser->GetHost()->AddDevToolsMessageObserver(observer);
  ScopedJNIRegistration jregistration(env, registration);
  return jregistration.Release();
}
JNIEXPORT jlong JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetWindowHandle(JNIEnv* env,
                                                      jobject obj,
                                                      jlong displayHandle) {
  CefWindowHandle windowHandle = kNullWindowHandle;
#if defined(OS_WIN)
  windowHandle = ::WindowFromDC((HDC)displayHandle);
#elif defined(OS_LINUX)
  return displayHandle;
#elif defined(OS_MACOSX)
  ASSERT(util_mac::IsNSView((void*)displayHandle));
#endif
  return (jlong)windowHandle;
}
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1CanGoBack(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->CanGoBack() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GoBack(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GoBack();
}
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1CanGoForward(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->CanGoForward() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GoForward(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GoForward();
}
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1IsLoading(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->IsLoading() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1Reload(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->Reload();
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1ReloadIgnoreCache(JNIEnv* env,
                                                        jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->ReloadIgnoreCache();
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1StopLoad(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->StopLoad();
}
JNIEXPORT jint JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetIdentifier(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, -1);
  return browser->GetIdentifier();
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetMainFrame(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetMainFrame();
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFocusedFrame(JNIEnv* env,
                                                      jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetFocusedFrame();
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFrameByIdentifier(JNIEnv* env,
                                                           jobject obj,
                                                           jstring identifier) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame =
      browser->GetFrameByIdentifier(GetJNIString(env, identifier));
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFrameByName(JNIEnv* env,
                                                     jobject obj,
                                                     jstring name) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetFrameByName(GetJNIString(env, name));
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}
JNIEXPORT jint JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFrameCount(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, -1);
  return (jint)browser->GetFrameCount();
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFrameIdentifiers(JNIEnv* env,
                                                          jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  std::vector<CefString> identifiers;
  browser->GetFrameIdentifiers(identifiers);
  return NewJNIStringVector(env, identifiers);
}
JNIEXPORT jobject JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetFrameNames(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  std::vector<CefString> names;
  browser->GetFrameNames(names);
  return NewJNIStringVector(env, names);
}
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1IsPopup(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->IsPopup() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1HasDocument(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->HasDocument() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1ViewSource(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefRefPtr<CefFrame> mainFrame = browser->GetMainFrame();
  CefPostTask(TID_UI, base::BindOnce(&CefFrame::ViewSource, mainFrame.get()));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetSource(JNIEnv* env,
                                                jobject obj,
                                                jobject jvisitor) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->GetSource(new StringVisitor(env, jvisitor));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetText(JNIEnv* env,
                                              jobject obj,
                                              jobject jvisitor) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->GetText(new StringVisitor(env, jvisitor));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1LoadRequest(JNIEnv* env,
                                                  jobject obj,
                                                  jobject jrequest) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  ScopedJNIRequest requestObj(env);
  requestObj.SetHandle(jrequest, false /* should_delete */);
  CefRefPtr<CefRequest> request = requestObj.GetCefObject();
  if (!request)
    return;
  browser->GetMainFrame()->LoadRequest(request);
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1LoadURL(JNIEnv* env,
                                              jobject obj,
                                              jstring url) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->LoadURL(GetJNIString(env, url));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1ExecuteJavaScript(JNIEnv* env,
                                                        jobject obj,
                                                        jstring code,
                                                        jstring url,
                                                        jint line) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->ExecuteJavaScript(GetJNIString(env, code),
                                             GetJNIString(env, url), line);
}
JNIEXPORT jstring JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetURL(JNIEnv* env, jobject obj) {
  jstring tmp = NewJNIString(env, "");
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, tmp);
  return NewJNIString(env, browser->GetMainFrame()->GetURL());
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1Close(JNIEnv* env,
                                            jobject obj,
                                            jboolean force) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (force != JNI_FALSE) {
    if (browser->GetHost()->IsWindowRenderingDisabled()) {
      browser->GetHost()->CloseBrowser(true);
    } else {
      // Destroy the native window representation.
      if (CefCurrentlyOn(TID_UI))
        util::DestroyCefBrowser(browser);
      else
        CefPostTask(TID_UI, base::BindOnce(&util::DestroyCefBrowser, browser));
    }
  } else {
    browser->GetHost()->CloseBrowser(false);
  }
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SetFocus(JNIEnv* env,
                                               jobject obj,
                                               jboolean enable) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->SetFocus(enable != JNI_FALSE);
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SetWindowVisibility(JNIEnv* env,
                                                          jobject obj,
                                                          jboolean visible) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
#if defined(OS_MACOSX)
  if (!browser->GetHost()->IsWindowRenderingDisabled()) {
    util_mac::SetVisibility(browser->GetHost()->GetWindowHandle(),
                            visible != JNI_FALSE);
  }
#endif
}
JNIEXPORT jdouble JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetZoomLevel(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, 0.0);
  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  double result = 0.0;
  if (CefCurrentlyOn(TID_UI))
    result = host->GetZoomLevel();
  else {
    CriticalLock lock;
    CriticalWait waitCond(&lock);
    lock.Lock();
    CefPostTask(TID_UI, base::BindOnce(getZoomLevel, host, &waitCond, &result));
    waitCond.Wait(1000);
    lock.Unlock();
  }
  return result;
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SetZoomLevel(JNIEnv* env,
                                                   jobject obj,
                                                   jdouble zoom) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->SetZoomLevel(zoom);
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1RunFileDialog(JNIEnv* env,
                                                    jobject obj,
                                                    jobject jmode,
                                                    jstring jtitle,
                                                    jstring jdefaultFilePath,
                                                    jobject jacceptFilters,
                                                    jint selectedAcceptFilter,
                                                    jobject jcallback) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  std::vector<CefString> accept_types;
  GetJNIStringVector(env, jacceptFilters, accept_types);
  CefBrowserHost::FileDialogMode mode;
  if (IsJNIEnumValue(env, jmode,
                     "com/techsenger/ceffx/core/handler/CefDialogHandler$FileDialogMode",
                     "FILE_DIALOG_OPEN")) {
    mode = FILE_DIALOG_OPEN;
  } else if (IsJNIEnumValue(env, jmode,
                            "com/techsenger/ceffx/core/handler/CefDialogHandler$FileDialogMode",
                            "FILE_DIALOG_OPEN_MULTIPLE")) {
    mode = FILE_DIALOG_OPEN_MULTIPLE;
  } else if (IsJNIEnumValue(env, jmode,
                            "com/techsenger/ceffx/core/handler/CefDialogHandler$FileDialogMode",
                            "FILE_DIALOG_OPEN_FOLDER")) {
    mode = FILE_DIALOG_OPEN_FOLDER;
  } else if (IsJNIEnumValue(env, jmode,
                            "com/techsenger/ceffx/core/handler/CefDialogHandler$FileDialogMode",
                            "FILE_DIALOG_SAVE")) {
    mode = FILE_DIALOG_SAVE;
  } else {
    mode = FILE_DIALOG_OPEN;
  }
  browser->GetHost()->RunFileDialog(
      mode, GetJNIString(env, jtitle), GetJNIString(env, jdefaultFilePath),
      accept_types, new RunFileDialogCallback(env, jcallback));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1StartDownload(JNIEnv* env,
                                                    jobject obj,
                                                    jstring url) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->StartDownload(GetJNIString(env, url));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1Print(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Print();
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1PrintToPDF(JNIEnv* env,
                                                 jobject obj,
                                                 jstring jpath,
                                                 jobject jsettings,
                                                 jobject jcallback) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefPdfPrintSettings settings = GetJNIPdfPrintSettings(env, jsettings);
  browser->GetHost()->PrintToPDF(GetJNIString(env, jpath), settings,
                                 new PdfPrintCallback(env, jcallback));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1Find(JNIEnv* env,
                                           jobject obj,
                                           jstring searchText,
                                           jboolean forward,
                                           jboolean matchCase,
                                           jboolean findNext) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Find(GetJNIString(env, searchText),
                           (forward != JNI_FALSE), (matchCase != JNI_FALSE),
                           (findNext != JNI_FALSE));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1StopFinding(JNIEnv* env,
                                                  jobject obj,
                                                  jboolean clearSelection) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->StopFinding(clearSelection != JNI_FALSE);
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1CloseDevTools(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->CloseDevTools();
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1ReplaceMisspelling(JNIEnv* env,
                                                         jobject obj,
                                                         jstring jword) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->ReplaceMisspelling(GetJNIString(env, jword));
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1WasResized(JNIEnv* env,
                                                 jobject obj,
                                                 jint width,
                                                 jint height) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (browser->GetHost()->IsWindowRenderingDisabled()) {
    browser->GetHost()->WasResized();
  }
#if (defined(OS_WIN) || defined(OS_LINUX))
  else {
    CefWindowHandle browserHandle = browser->GetHost()->GetWindowHandle();
    if (CefCurrentlyOn(TID_UI)) {
      util::SetWindowSize(browserHandle, width, height);
    } else {
      CefPostTask(TID_UI, base::BindOnce(util::SetWindowSize, browserHandle,
                                         (int)width, (int)height));
    }
  }
#endif
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1Invalidate(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Invalidate(PET_VIEW);
}

// ============================================================================
// SendKeyEvent - Handle JavaFX KeyEvent and send it to CEF browser
// ============================================================================

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SendKeyEvent(
    JNIEnv* env,
    jobject obj,
    jobject key_event) {

  // Get the CEF browser instance
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (!browser) {
    return;
  }

  // Get the KeyEvent class
  ScopedJNIClass cls(env, env->GetObjectClass(key_event));
  if (!cls) {
    return;
  }

  // Get method IDs for JavaFX KeyEvent
  jmethodID getEventType_mid = env->GetMethodID(cls, "getEventType", "()Ljavafx/event/EventType;");
  jmethodID getCode_mid = env->GetMethodID(cls, "getCode", "()Ljavafx/scene/input/KeyCode;");
  jmethodID getCharacter_mid = env->GetMethodID(cls, "getCharacter", "()Ljava/lang/String;");
  jmethodID isControlDown_mid = env->GetMethodID(cls, "isControlDown", "()Z");
  jmethodID isShiftDown_mid = env->GetMethodID(cls, "isShiftDown", "()Z");
  jmethodID isAltDown_mid = env->GetMethodID(cls, "isAltDown", "()Z");
  jmethodID isMetaDown_mid = env->GetMethodID(cls, "isMetaDown", "()Z");

  // Get the event type object
  jobject event_type_obj = env->CallObjectMethod(key_event, getEventType_mid);
  // Get static EventType fields from KeyEvent class
  jclass key_event_class = env->FindClass("javafx/scene/input/KeyEvent");
  if (!key_event_class) {
    env->DeleteLocalRef(event_type_obj);
    return;
  }

  // Get static field IDs for KEY_PRESSED, KEY_RELEASED, KEY_TYPED
  jfieldID key_pressed_fid = env->GetStaticFieldID(key_event_class, "KEY_PRESSED", "Ljavafx/event/EventType;");
  jfieldID key_released_fid = env->GetStaticFieldID(key_event_class, "KEY_RELEASED", "Ljavafx/event/EventType;");
  jfieldID key_typed_fid = env->GetStaticFieldID(key_event_class, "KEY_TYPED", "Ljavafx/event/EventType;");

  if (!key_pressed_fid || !key_released_fid || !key_typed_fid) {
    env->DeleteLocalRef(event_type_obj);
    env->DeleteLocalRef(key_event_class);
    return;
  }

  // Get the actual static objects
  jobject key_pressed_obj = env->GetStaticObjectField(key_event_class, key_pressed_fid);
  jobject key_released_obj = env->GetStaticObjectField(key_event_class, key_released_fid);
  jobject key_typed_obj = env->GetStaticObjectField(key_event_class, key_typed_fid);

  // Determine the CEF event type based on the JavaFX event type
  cef_key_event_type_t cef_event_type = KEYEVENT_RAWKEYDOWN;

  if (env->IsSameObject(event_type_obj, key_pressed_obj)) {
    cef_event_type = KEYEVENT_RAWKEYDOWN;
  } else if (env->IsSameObject(event_type_obj, key_released_obj)) {
    cef_event_type = KEYEVENT_KEYUP;
  } else if (env->IsSameObject(event_type_obj, key_typed_obj)) {
    cef_event_type = KEYEVENT_CHAR;
  } else {
    env->DeleteLocalRef(event_type_obj);
    env->DeleteLocalRef(key_pressed_obj);
    env->DeleteLocalRef(key_released_obj);
    env->DeleteLocalRef(key_typed_obj);
    env->DeleteLocalRef(key_event_class);
    return;
  }

  // Get KeyCode enum object
  jobject key_code_obj = env->CallObjectMethod(key_event, getCode_mid);
  if (!key_code_obj) {
    env->DeleteLocalRef(event_type_obj);
    env->DeleteLocalRef(key_pressed_obj);
    env->DeleteLocalRef(key_released_obj);
    env->DeleteLocalRef(key_typed_obj);
    env->DeleteLocalRef(key_event_class);
    return;
  }

  // Get the KeyCode class
  ScopedJNIClass key_code_cls(env, env->GetObjectClass(key_code_obj));
  if (!key_code_cls) {
    env->DeleteLocalRef(event_type_obj);
    env->DeleteLocalRef(key_pressed_obj);
    env->DeleteLocalRef(key_released_obj);
    env->DeleteLocalRef(key_typed_obj);
    env->DeleteLocalRef(key_event_class);
    env->DeleteLocalRef(key_code_obj);
    return;
  }

  // Get the code value from KeyCode enum (getCode() method)
  jmethodID key_code_get_code_mid =
      env->GetMethodID(key_code_cls, "getCode", "()I");
  if (!key_code_get_code_mid) {
    env->DeleteLocalRef(event_type_obj);
    env->DeleteLocalRef(key_pressed_obj);
    env->DeleteLocalRef(key_released_obj);
    env->DeleteLocalRef(key_typed_obj);
    env->DeleteLocalRef(key_event_class);
    env->DeleteLocalRef(key_code_obj);
    return;
  }

  // Call getCode() to get the JavaFX KeyCode value
  int javafx_key_code = env->CallIntMethod(key_code_obj, key_code_get_code_mid);

  // Get the character string from the key event
  jstring character_str =
      (jstring)env->CallObjectMethod(key_event, getCharacter_mid);

  char16_t key_char = 0;
  if (character_str) {
    const jchar* chars = env->GetStringChars(character_str, nullptr);
    if (chars && env->GetStringLength(character_str) > 0) {
      key_char = chars[0];
      env->ReleaseStringChars(character_str, chars);
    }
    env->DeleteLocalRef(character_str);
  }

  // Get modifier key states
  jboolean is_ctrl_down = env->CallBooleanMethod(key_event, isControlDown_mid);
  jboolean is_shift_down = env->CallBooleanMethod(key_event, isShiftDown_mid);
  jboolean is_alt_down = env->CallBooleanMethod(key_event, isAltDown_mid);
  jboolean is_meta_down = env->CallBooleanMethod(key_event, isMetaDown_mid);

  // Build CEF modifiers from JavaFX modifiers
  int cef_modifiers = 0;
  if (is_ctrl_down)
    cef_modifiers |= EVENTFLAG_CONTROL_DOWN;
  if (is_shift_down)
    cef_modifiers |= EVENTFLAG_SHIFT_DOWN;
  if (is_alt_down)
    cef_modifiers |= EVENTFLAG_ALT_DOWN;
  if (is_meta_down)
    cef_modifiers |= EVENTFLAG_COMMAND_DOWN;

  // Create CEF key event
  CefKeyEvent cef_event;
  cef_event.type = cef_event_type;
  cef_event.modifiers = cef_modifiers;

#if defined(OS_WIN)

  // Map JavaFX KeyCode to Windows virtual key code
  cef_event.windows_key_code = JavaFXKeyCodeToWindowsKeyCode(javafx_key_code);

  // For KEY_PRESSED and KEY_RELEASED, include scan code
  if (cef_event_type == KEYEVENT_RAWKEYDOWN ||
      cef_event_type == KEYEVENT_KEYUP) {
    int scan_code =
        MapVirtualKey(cef_event.windows_key_code, MAPVK_VK_TO_VSC);
    cef_event.native_key_code = (scan_code << 16) | 1;

    // For KEY_RELEASED, set bits 30 and 31
    if (cef_event_type == KEYEVENT_KEYUP) {
      cef_event.native_key_code |= 0xC0000000;
    }
  } else if (cef_event_type == KEYEVENT_CHAR) {
    cef_event.windows_key_code = key_char;
  }

#elif defined(OS_LINUX)
  // Map JavaFX KeyCode to X11 keysym
  unsigned int x11_keysym = JavaFXKeyCodeToXKeysym(javafx_key_code);
  cef_event.native_key_code = x11_keysym;

  // Convert X11 keysym to Windows key code for CEF
  KeyboardCode windows_key_code = KeyboardCodeFromXKeysym(cef_event.native_key_code);
  cef_event.windows_key_code = GetWindowsKeyCodeWithoutLocation(windows_key_code);

  // Set system key flag for Alt
  if (cef_event.modifiers & EVENTFLAG_ALT_DOWN)
    cef_event.is_system_key = true;

  // Set character fields
  if (windows_key_code == VKEY_RETURN) {
    cef_event.unmodified_character = '\r';
  } else {
    cef_event.unmodified_character =
        key_char ? key_char : cef_event.native_key_code;
  }

  // Handle control characters
  if (cef_event.modifiers & EVENTFLAG_CONTROL_DOWN) {
    cef_event.character = GetControlCharacter(windows_key_code, cef_event.modifiers & EVENTFLAG_SHIFT_DOWN);
  } else {
    cef_event.character = cef_event.unmodified_character;
  }

#elif defined(OS_MACOSX)
  // Map JavaFX KeyCode to Mac key code
  cef_event.native_key_code = JavaFXKeyCodeToMacKeyCode(javafx_key_code);
  cef_event.unmodified_character = key_char;
  cef_event.character = key_char;

  // Handle shift key character transformations
  if (cef_event.modifiers & EVENTFLAG_SHIFT_DOWN) {
    if (key_char >= '0' && key_char <= '9') {
      cef_event.character = kShiftCharsForNumberKeys[key_char - '0'];
    } else if (key_char >= 'a' && key_char <= 'z') {
      cef_event.character = 'A' + (key_char - 'a');
    } else {
      cef_event.character = GetMacShiftCharacter(cef_event.native_key_code);
    }
  }

  // Handle control characters
  if (cef_event.modifiers & EVENTFLAG_CONTROL_DOWN) {
    if (key_char >= 'A' && key_char <= 'Z')
      cef_event.character = 1 + key_char - 'A';
    else if (key_char >= 'a' && key_char <= 'z')
      cef_event.character = 1 + key_char - 'a';
    else
      cef_event.character = GetMacControlCharacter(cef_event.native_key_code);
  }
#endif
  // Send the key event to the browser
  browser->GetHost()->SendKeyEvent(cef_event);

  // Clean up all local JNI references
  env->DeleteLocalRef(event_type_obj);
  env->DeleteLocalRef(key_pressed_obj);
  env->DeleteLocalRef(key_released_obj);
  env->DeleteLocalRef(key_typed_obj);
  env->DeleteLocalRef(key_event_class);
  env->DeleteLocalRef(key_code_obj);
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SendMouseEvent(JNIEnv* env,
                                                     jobject obj,
                                                     jobject mouse_event) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  jclass mouseEventClass = env->GetObjectClass(mouse_event);
  if (!mouseEventClass)
    return;

  jmethodID getXMethod = env->GetMethodID(mouseEventClass, "getX", "()D");
  jmethodID getYMethod = env->GetMethodID(mouseEventClass, "getY", "()D");

  if (!getXMethod || !getYMethod)
    return;

  jdouble x = env->CallDoubleMethod(mouse_event, getXMethod);
  jdouble y = env->CallDoubleMethod(mouse_event, getYMethod);

  CefMouseEvent cef_event;
  cef_event.x = (int)x;
  cef_event.y = (int)y;
  cef_event.modifiers = GetCefModifiersFromJavaFXMouse(env, mouse_event);

  jclass eventClass = env->FindClass("javafx/event/Event");
  if (!eventClass)
    return;

  jmethodID getEventTypeMethod = env->GetMethodID(eventClass, "getEventType", "()Ljavafx/event/EventType;");
  if (!getEventTypeMethod)
    return;

  jobject eventType = env->CallObjectMethod(mouse_event, getEventTypeMethod);
  if (!eventType)
    return;

  jclass eventTypeClass = env->GetObjectClass(eventType);

  jmethodID toStringMethod = env->GetMethodID(
      eventTypeClass,
      "toString",
      "()Ljava/lang/String;"
  );

  if (toStringMethod) {
    jstring eventTypeStr =
        (jstring)env->CallObjectMethod(eventType, toStringMethod);

    const char* eventTypeStrC =
        env->GetStringUTFChars(eventTypeStr, NULL);

    CefBrowserHost::MouseButtonType button = MBT_LEFT;

    jmethodID getButtonMethod = env->GetMethodID(
        mouseEventClass,
        "getButton",
        "()Ljavafx/scene/input/MouseButton;"
    );

    if (getButtonMethod) {
      jobject mouseButton = env->CallObjectMethod(mouse_event, getButtonMethod);

      if (mouseButton) {
        jclass mouseButtonClass = env->GetObjectClass(mouseButton);
        jmethodID mouseButtonToStringMethod =
            env->GetMethodID(mouseButtonClass, "toString", "()Ljava/lang/String;");

        if (mouseButtonToStringMethod) {
          jstring mouseButtonStr =
              (jstring)env->CallObjectMethod(mouseButton, mouseButtonToStringMethod);

          if (mouseButtonStr) {
            const char* mouseButtonStrC =
                env->GetStringUTFChars(mouseButtonStr, NULL);

            if (strcmp(mouseButtonStrC, "SECONDARY") == 0) {
              button = MBT_RIGHT;
            } else if (strcmp(mouseButtonStrC, "MIDDLE") == 0) {
              button = MBT_MIDDLE;
            }

            env->ReleaseStringUTFChars(mouseButtonStr, mouseButtonStrC);
          }
        }
      }
    }

    if (strstr(eventTypeStrC, "MOUSE_PRESSED")) {
      browser->GetHost()->SendMouseClickEvent(
          cef_event,
          button,
          false,
          1
      );
    } else if (strstr(eventTypeStrC, "MOUSE_RELEASED")) {
      browser->GetHost()->SendMouseClickEvent(
          cef_event,
          button,
          true,
          1
      );
    } else {
      browser->GetHost()->SendMouseMoveEvent(
          cef_event,
          false
      );
    }

    env->ReleaseStringUTFChars(
        eventTypeStr,
        eventTypeStrC
    );
  }
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SendMouseWheelEvent(JNIEnv* env,
                                                        jobject obj,
                                                        jobject scroll_event) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  jclass scrollEventClass = env->GetObjectClass(scroll_event);
  if (!scrollEventClass)
    return;

  jmethodID getXMethod = env->GetMethodID(scrollEventClass, "getX", "()D");
  jmethodID getYMethod = env->GetMethodID(scrollEventClass, "getY", "()D");

  if (!getXMethod || !getYMethod)
    return;

  jdouble x = env->CallDoubleMethod(scroll_event, getXMethod);
  jdouble y = env->CallDoubleMethod(scroll_event, getYMethod);

  jmethodID getDeltaYMethod = env->GetMethodID(scrollEventClass, "getDeltaY", "()D");
  if (!getDeltaYMethod)
    return;

  jdouble deltaY = env->CallDoubleMethod(scroll_event, getDeltaYMethod);

  jmethodID getDeltaXMethod = env->GetMethodID(scrollEventClass, "getDeltaX", "()D");
  jdouble deltaX = getDeltaXMethod ? env->CallDoubleMethod(scroll_event, getDeltaXMethod) : 0.0;

  CefMouseEvent cef_event;
  cef_event.x = (int)x;
  cef_event.y = (int)y;
  cef_event.modifiers = GetCefModifiersFromJavaFXInput(env, scroll_event);

  int wheel_delta_y = (int)(deltaY * 1.5);
  int wheel_delta_x = (int)(deltaX * 1.5);

  browser->GetHost()->SendMouseWheelEvent(cef_event, wheel_delta_x, wheel_delta_y);
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragTargetDragEnter(
    JNIEnv* env,
    jobject obj,
    jobject jdragData,
    jobject jpoint,
    jint jmodifiers,
    jint allowedOps) {

  CefRefPtr<CefDragData> drag_data =
      GetCefFromJNIObject<CefDragData>(env, jdragData, "CefDragData");

  if (!drag_data)
    return;

  CefMouseEvent cef_event;

  // JavaFX Point2D -> CefMouseEvent (double -> int)
  GetJNIPoint2D(env, jpoint, &cef_event.x, &cef_event.y);

  cef_event.modifiers = static_cast<int>(
      GetCefModifiers(env, nullptr, jmodifiers));

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  browser->GetHost()->DragTargetDragEnter(
      drag_data,
      cef_event,
      static_cast<CefBrowserHost::DragOperationsMask>(allowedOps));
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragTargetDragOver(
    JNIEnv* env,
    jobject obj,
    jobject pos,
    jint jmodifiers,
    jint allowedOps) {

  CefMouseEvent cef_event;
  // JavaFX Point2D -> CefMouseEvent
  GetJNIPoint2D(env, pos, &cef_event.x, &cef_event.y);
  cef_event.modifiers = GetCefModifiers(env, nullptr, jmodifiers);
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDragOver(
      cef_event,
      static_cast<CefBrowserHost::DragOperationsMask>(allowedOps));
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragTargetDragLeave(JNIEnv* env,
                                                          jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDragLeave();
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragTargetDrop(
    JNIEnv* env,
    jobject obj,
    jobject pos,
    jint jmodifiers) {

  CefMouseEvent cef_event;
  // JavaFX Point2D -> CefMouseEvent
  GetJNIPoint2D(env, pos, &cef_event.x, &cef_event.y);
  cef_event.modifiers = GetCefModifiers(env, nullptr, jmodifiers);
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDrop(cef_event);
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragSourceEndedAt(
    JNIEnv* env,
    jobject obj,
    jobject pos,
    jint operation) {

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefMouseEvent cef_event;
  // JavaFX Point2D -> CefMouseEvent
  GetJNIPoint2D(env, pos, &cef_event.x, &cef_event.y);
  browser->GetHost()->DragSourceEndedAt(
      cef_event.x,
      cef_event.y,
      static_cast<CefBrowserHost::DragOperationsMask>(operation));
}

JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1DragSourceSystemDragEnded(JNIEnv* env,
                                                                jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragSourceSystemDragEnded();
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1UpdateUI(JNIEnv* env,
                                               jobject obj,
                                               jobject jcontentRect,
                                               jobject jbrowserRect) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefRect contentRect = GetJNIRect(env, jcontentRect);
#if defined(OS_MACOSX)
  CefRect browserRect = GetJNIRect(env, jbrowserRect);
  util_mac::UpdateView(browser->GetHost()->GetWindowHandle(), contentRect,
                       browserRect);
#else
  CefWindowHandle windowHandle = browser->GetHost()->GetWindowHandle();
  if (CefCurrentlyOn(TID_UI)) {
    util::SetWindowBounds(windowHandle, contentRect);
  } else {
    CefPostTask(TID_UI, base::BindOnce(util::SetWindowBounds, windowHandle,
                                       contentRect));
  }
#endif
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SetParent(JNIEnv* env,
                                                jobject obj,
                                                jlong windowHandle) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  base::OnceClosure callback = base::BindOnce(&OnAfterParentChanged, browser);
#if defined(OS_MACOSX)
  util::SetParent(browser->GetHost()->GetWindowHandle(), windowHandle,
                  std::move(callback));
#else
  CefWindowHandle browserHandle = browser->GetHost()->GetWindowHandle();
  CefWindowHandle parentHandle =
      windowHandle ? (CefWindowHandle)windowHandle : kNullWindowHandle;
  if (CefCurrentlyOn(TID_UI)) {
    util::SetParent(browserHandle, parentHandle, std::move(callback));
  } else {
#if defined(OS_LINUX)
    CriticalLock lock;
    CriticalWait waitCond(&lock);
    lock.Lock();
    CefPostTask(TID_UI,
                base::BindOnce(util::SetParentSync, browserHandle, parentHandle,
                               &waitCond, std::move(callback)));
    waitCond.Wait(1000);
    lock.Unlock();
#else
    CefPostTask(TID_UI, base::BindOnce(util::SetParent, browserHandle,
                                       parentHandle, std::move(callback)));
#endif
  }
#endif
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1NotifyMoveOrResizeStarted(JNIEnv* env,
                                                                jobject obj) {
#if (defined(OS_WIN) || defined(OS_LINUX))
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (!browser->GetHost()->IsWindowRenderingDisabled()) {
    browser->GetHost()->NotifyMoveOrResizeStarted();
  }
#endif
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1SetWindowlessFrameRate(JNIEnv* env,
                                                             jobject jbrowser,
                                                             jint frameRate) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, jbrowser);
  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  host->SetWindowlessFrameRate(frameRate);
}
void getWindowlessFrameRate(CefRefPtr<CefBrowserHost> host,
                            CefRefPtr<IntCallback> callback) {
  callback->onComplete((jint)host->GetWindowlessFrameRate());
}
JNIEXPORT void JNICALL
Java_com_techsenger_ceffx_core_browser_CefBrowser_1N_N_1GetWindowlessFrameRate(
    JNIEnv* env,
    jobject jbrowser,
    jobject jintCallback) {
  CefRefPtr<IntCallback> callback = new IntCallback(env, jintCallback);
  CefRefPtr<CefBrowser> browser = GetJNIBrowser(env, jbrowser);
  if (!browser.get()) {
    callback->onComplete(0);
    return;
  }
  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  if (CefCurrentlyOn(TID_UI)) {
    getWindowlessFrameRate(host, callback);
  } else {
    CefPostTask(TID_UI, base::BindOnce(getWindowlessFrameRate, host, callback));
  }
}
