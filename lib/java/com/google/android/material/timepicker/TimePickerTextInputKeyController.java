/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.timepicker;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_NEXT;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.TimePickerControls.ActiveSelection;

/**
 * A class for the keyboard logic when the TimePicker is in {@code TimeFormat.KEYBOARD}
 *
 * <p>Controls part of the input validation and focus switching.
 */
class TimePickerTextInputKeyController implements OnEditorActionListener, OnKeyListener {

  private final ChipTextInputComboView hourLayoutComboView;
  private final ChipTextInputComboView minuteLayoutComboView;
  private final ChipTextInputComboView secondLayoutComboView;
  private final TimeModel time;

  private boolean keyListenerRunning = false;
  private boolean isSecondEnabled = true; // TODO SecTime

  TimePickerTextInputKeyController(
      ChipTextInputComboView hourLayoutComboView,
      ChipTextInputComboView minuteLayoutComboView,
      ChipTextInputComboView secondLayoutComboView,
      TimeModel time) {
    this.hourLayoutComboView = hourLayoutComboView;
    this.minuteLayoutComboView = minuteLayoutComboView;
    this.secondLayoutComboView = secondLayoutComboView;
    this.time = time;
  }

  /** Prepare Text inputs to receive key events and IME actions. */
  public void bind() {
    TextInputLayout hourLayout = hourLayoutComboView.getTextInput();
    TextInputLayout minuteLayout = minuteLayoutComboView.getTextInput();
    TextInputLayout secondLayout = secondLayoutComboView.getTextInput();
    EditText hourEditText = hourLayout.getEditText();
    EditText minuteEditText = minuteLayout.getEditText();
    EditText secondEditText = secondLayout.getEditText();

    hourEditText.setImeOptions(IME_ACTION_NEXT | IME_FLAG_NO_EXTRACT_UI);
    minuteEditText.setImeOptions((isSecondEnabled ? IME_ACTION_NEXT : IME_ACTION_DONE) | IME_FLAG_NO_EXTRACT_UI);
    secondEditText.setImeOptions(IME_ACTION_DONE | IME_FLAG_NO_EXTRACT_UI);

    hourEditText.setOnEditorActionListener(this);
    if (isSecondEnabled) {
      minuteEditText.setOnEditorActionListener(this);
    }
    hourEditText.setOnKeyListener(this);
    minuteEditText.setOnKeyListener(this);
    secondEditText.setOnKeyListener(this);
  }

  private void moveSelection(@ActiveSelection int selection) {
    secondLayoutComboView.setChecked(selection == SECOND);
    minuteLayoutComboView.setChecked(selection == MINUTE);
    hourLayoutComboView.setChecked(selection == HOUR);
    time.selection = selection;
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    boolean actionNext = actionId == IME_ACTION_NEXT;
    if (actionNext) {
      switch (time.selection) {
        case HOUR:
          moveSelection(MINUTE);
        case MINUTE:
          if (isSecondEnabled) moveSelection(SECOND);
      }
    }

    return actionNext;
  }

  @Override
  public boolean onKey(View view, int keyCode, KeyEvent event) {
    if (keyListenerRunning) {
      return false;
    }

    keyListenerRunning = true;
    EditText editText = (EditText) view;
    boolean ret =
        time.selection == SECOND
            ? onSecondKeyPress(keyCode, event, editText)
            : time.selection == MINUTE
            ? onMinuteKeyPress(keyCode, event, editText)
            : onHourKeyPress(keyCode, event, editText);
    keyListenerRunning = false;
    return ret;
  }

  private boolean onSecondKeyPress(int keyCode, KeyEvent event, EditText editText) {
    boolean switchFocus =
        keyCode == KeyEvent.KEYCODE_DEL
            && event.getAction() == KeyEvent.ACTION_DOWN
            && TextUtils.isEmpty(editText.getText());
    if (switchFocus) {
      moveSelection(MINUTE);
      return true;
    }

    clearPrefilledText(editText);

    return false;
  }

  private boolean onMinuteKeyPress(int keyCode, KeyEvent event, EditText editText) {
    boolean switchFocusToHour =
        keyCode == KeyEvent.KEYCODE_DEL
            && event.getAction() == KeyEvent.ACTION_DOWN
            && TextUtils.isEmpty(editText.getText());

    // Auto-switch focus when 2 numbers are successfully entered
    boolean switchFocusToSeconds =
        isSecondEnabled
            && keyCode >= KeyEvent.KEYCODE_0
            && keyCode <= KeyEvent.KEYCODE_9
            && event.getAction() == KeyEvent.ACTION_UP
            && editText.getSelectionStart() == 2
            && !TextUtils.isEmpty(editText.getText())
            && editText.getText().length() == 2;

    if (switchFocusToHour) {
      moveSelection(HOUR);
      return true;
    } else if (switchFocusToSeconds) {
      moveSelection(SECOND);
      return true;
    }

    clearPrefilledText(editText);

    return false;
  }

  private boolean onHourKeyPress(int keyCode, KeyEvent event, EditText editText) {
    Editable text = editText.getText();
    if (text == null) {
      return false;
    }

    // Auto-switch focus when 2 numbers are successfully entered
    boolean switchFocus =
        keyCode >= KeyEvent.KEYCODE_0
            && keyCode <= KeyEvent.KEYCODE_9
            && event.getAction() == KeyEvent.ACTION_UP
            && editText.getSelectionStart() == 2
            && text.length() == 2;
    if (switchFocus) {
      moveSelection(MINUTE);
      return true;
    }

    clearPrefilledText(editText);

    return false;
  }

  // Improve UX by auto-clearing existing text when entering new time
  private void clearPrefilledText(EditText editText) {
    if (editText.getSelectionStart() == 0 && editText.length() == 2) {
      editText.getText().clear();
    }
  }
}
