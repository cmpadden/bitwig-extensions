package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.oldframework.targets.ButtonTarget;
import com.bitwig.extensions.oldframework.targets.EncoderTarget;
import com.bitwig.extensions.oldframework.targets.RGBButtonTarget;
import com.bitwig.extensions.util.NoteInputUtils;

public class PresonusAtom2 extends ControllerExtension
{
   final static int CC_ENCODER_1 = 0x0E;

   final static int CC_ENCODER_2 = 0x0F;

   final static int CC_ENCODER_3 = 0x10;

   final static int CC_ENCODER_4 = 0x11;

   final static int CC_SHIFT = 0x20;

   final static int CC_NOTE_REPEAT = 0x18;

   final static int CC_FULL_LEVEL = 0x19;

   final static int CC_BANK_TRANSPOSE = 0x1A;

   final static int CC_PRESET_PAD_SELECT = 0x1B;

   final static int CC_SHOW_HIDE = 0x1D;

   final static int CC_NUDGE_QUANTIZE = 0x1E;

   final static int CC_EDITOR = 0x1F;

   final static int CC_SET_LOOP = 0x55;

   final static int CC_SETUP = 0x56;

   final static int CC_UP = 0x57;

   final static int CC_DOWN = 0x59;

   final static int CC_LEFT = 0x5A;

   final static int CC_RIGHT = 0x66;

   final static int CC_SELECT = 0x67;

   final static int CC_ZOOM = 0x68;

   final static int CC_CLICK_COUNT_IN = 0x69;

   final static int CC_RECORD_SAVE = 0x6B;

   final static int CC_PLAY_LOOP_TOGGLE = 0x6D;

   final static int CC_STOP_UNDO = 0x6F;

   final static int LAUNCHER_SCENES = 16;

   float[] WHITE = { 1, 1, 1 };

   float[] DIM_WHITE = { 0.3f, 0.3f, 0.3f };

   float[] BLACK = { 0, 0, 0 };

   float[] RED = { 1, 0, 0 };

   float[] DIM_RED = { 0.3f, 0.0f, 0.0f };

   public PresonusAtom2(final PresonusAtomDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      final MidiIn midiIn = host.getMidiInPort(0);

      // midiIn.setMidiCallback(getMidiCallbackToUseForLayers());
      mNoteInput = midiIn.createNoteInput("Pads", "80????", "90????", "a0????");
      mNoteInput.setShouldConsumeEvents(false);
      mArpeggiator = mNoteInput.arpeggiator();
      mArpeggiator.isEnabled().markInterested();
      mArpeggiator.period().markInterested();
      mArpeggiator.shuffle().markInterested();
      mArpeggiator.usePressureToVelocity().set(true);

      mMidiOut = host.getMidiOutPort(0);
      mMidiIn = host.getMidiInPort(0);

      mCursorTrack = host.createCursorTrack(0, LAUNCHER_SCENES);
      mCursorTrack.arm().markInterested();
      mSceneBank = host.createSceneBank(LAUNCHER_SCENES);

      for (int s = 0; s < LAUNCHER_SCENES; s++)
      {
         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
         slot.color().markInterested();
         slot.isPlaying().markInterested();
         slot.isRecording().markInterested();
         slot.isPlaybackQueued().markInterested();
         slot.isRecordingQueued().markInterested();
         slot.hasContent().markInterested();

         final Scene scene = mSceneBank.getScene(s);
         scene.color().markInterested();
         scene.exists().markInterested();
      }

      mCursorDevice = mCursorTrack.createCursorDevice("ATOM", "Atom", 0,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 4);
      for (int i = 0; i < 4; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTransport.getPosition().markInterested();

      mCursorClip = host.createLauncherCursorClip(16, 1);
      mCursorClip.color().markInterested();
      mCursorClip.clipLauncherSlot().color().markInterested();
      mCursorClip.clipLauncherSlot().isPlaying().markInterested();
      mCursorClip.clipLauncherSlot().isRecording().markInterested();
      mCursorClip.clipLauncherSlot().isPlaybackQueued().markInterested();
      mCursorClip.clipLauncherSlot().isRecordingQueued().markInterested();
      mCursorClip.clipLauncherSlot().hasContent().markInterested();
      mCursorClip.getLoopLength().markInterested();
      mCursorClip.getLoopStart().markInterested();

      createHardwareSurface();

      initLayers();

      initPads();
      initButtons();
      initEncoders();

      // Turn on Native Mode
      mMidiOut.sendMidi(0x8f, 0, 127);
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   @Override
   public void exit()
   {
      // Turn off Native Mode
      mMidiOut.sendMidi(0x8f, 0, 0);
   }

   private void createHardwareSurface()
   {
      final ControllerHost host = getHost();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      surface.setPhysicalSize(202, 195);

      mShiftButton = createCCButton(CC_SHIFT);

      // NAV section
      mUpButton = createCCButton(CC_UP);
      mDownButton = createCCButton(CC_DOWN);
      mLeftButton = createCCButton(CC_LEFT);
      mRightButton = createCCButton(CC_RIGHT);
      mZoomButton = createCCButton(CC_ZOOM);

      // TRANS section
      mClickCountInButton = createCCButton(CC_CLICK_COUNT_IN);
      mRecordSaveButton = createCCButton(CC_RECORD_SAVE);
      mPlayLoopButton = createCCButton(CC_PLAY_LOOP_TOGGLE);
      mStopUndoButton = createCCButton(CC_STOP_UNDO);

      // SONG section
      mSetupButton = createCCButton(CC_SETUP);
      mSetLoopButton = createCCButton(CC_SET_LOOP);

      // EVENT section
      mEditorButton = createCCButton(CC_EDITOR);
      mNudgeQuantizeButton = createCCButton(CC_NUDGE_QUANTIZE);

      // INST section
      mShowHideButton = createCCButton(CC_SHOW_HIDE);
      mPresetPadSelectButton = createCCButton(CC_PRESET_PAD_SELECT);
      mBankButton = createCCButton(CC_BANK_TRANSPOSE);

      // MODE section
      mFullLevelButton = createCCButton(CC_FULL_LEVEL);
      mNoteRepeatButton = createCCButton(CC_NOTE_REPEAT);
   }

   private void initLayers()
   {
      initBaseLayer();
   }

   private void initBaseLayer()
   {
      // TODO Auto-generated method stub

   }

   private Layer createLayer(final String name)
   {
      return new Layer(mLayers, name)
      {
         @Override
         public void setIsActive(final boolean active)
         {
            final boolean shouldPlayDrums = !mStepsLayer.isActive() && !mNoteRepeatShiftLayer.isActive()
               && !mLauncherClipsLayer.isActive() && !mStepsZoomLayer.isActive()
               && !mStepsSetupLoopLayer.isActive();

            mNoteInput
               .setKeyTranslationTable(shouldPlayDrums ? NoteInputUtils.ALL_NOTES : NoteInputUtils.NO_NOTES);
         }
      };
   }

   private int velocityForPlayingNote(final int padIndex)
   {
      if (mPlayingNotes != null)
      {
         for (final PlayingNote playingNote : mPlayingNotes)
         {
            if (playingNote.pitch() == 36 + padIndex)
            {
               return playingNote.velocity();
            }
         }
      }

      return 0;
   }

   private void initPads()
   {
      mDrumPadBank = mCursorDevice.createDrumPadBank(16);
      mDrumPadBank.exists().markInterested();
      mCursorTrack.color().markInterested();

      mDrumPadColors = new float[16][3];

      final Pad[] pads = new Pad[16];
      final float darken = 0.7f;

      for (int i = 0; i < 16; i++)
      {
         final int padIndex = i;
         final Pad pad = addElement(new Pad(padIndex));
         pads[padIndex] = pad;
         final DrumPad drumPad = mDrumPadBank.getItemAt(padIndex);
         drumPad.exists().markInterested();
         final SettableColorValue color = drumPad.color();
         color.addValueObserver((r, g, b) -> {
            mDrumPadColors[padIndex][0] = r * darken;
            mDrumPadColors[padIndex][1] = g * darken;
            mDrumPadColors[padIndex][2] = b * darken;
         });

         mBaseLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               float[] drumPadColor = mDrumPadColors[padIndex];

               if (!mDrumPadBank.exists().get())
               {
                  final SettableColorValue c = mCursorTrack.color();
                  drumPadColor = new float[] { c.red(), c.green(), c.blue() };
               }

               final int playing = velocityForPlayingNote(padIndex);
               if (playing > 0)
               {
                  return mixColorWithWhite(drumPadColor, playing);
               }
               return drumPadColor;
            }

            @Override
            public boolean get()
            {
               return mDrumPadBank.exists().get() ? drumPad.exists().get() : true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  mCursorClip.scrollToKey(36 + padIndex);
                  mCurrentPadForSteps = padIndex;
               }
            }
         });
         mDrumPadColors[padIndex][0] = color.red() * darken;
         mDrumPadColors[padIndex][1] = color.green() * darken;
         mDrumPadColors[padIndex][2] = color.blue() * darken;

         mStepsLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {

               if (mShift)
               {
                  if (mCurrentPadForSteps == padIndex)
                  {
                     return WHITE;
                  }

                  final int playingNote = velocityForPlayingNote(padIndex);

                  if (playingNote > 0)
                  {
                     return mixColorWithWhite(clipColor(0.3f), playingNote);
                  }

                  return clipColor(0.3f);
               }

               if (mPlayingStep == padIndex + mCurrentPageForSteps * 16)
               {
                  return WHITE;
               }

               final boolean isNewNote = mStepData[padIndex] == 2;
               final boolean hasData = mStepData[padIndex] > 0;

               if (isNewNote)
                  return RGBButtonTarget.mixWithValue(mCursorClip.color(), WHITE, 0.5f);
               else if (hasData)
                  return RGBButtonTarget.getFromValue(mCursorClip.color());
               else
                  return RGBButtonTarget.mixWithValue(mCursorClip.color(), BLACK, 0.8f);
            }

            private float[] clipColor(final float scale)
            {
               final float[] color = new float[3];
               final SettableColorValue c = mCursorClip.color();
               color[0] = c.red() * scale;
               color[1] = c.green() * scale;
               color[2] = c.blue() * scale;
               return color;
            }

            @Override
            public boolean get()
            {
               return true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  if (mShift)
                  {
                     mCursorClip.scrollToKey(36 + padIndex);
                     mCurrentPadForSteps = padIndex;
                     mCursorTrack.playNote(36 + padIndex, 100);
                  }
                  else
                     mCursorClip.toggleStep(padIndex, 0, 100);
               }
            }
         });

         mLauncherClipsLayer.bind(pad, new RGBButtonTarget()
         {
            private ClipLauncherSlot getSlot()
            {
               return mCursorTrack.clipLauncherSlotBank().getItemAt(padIndex);
            }

            @Override
            public float[] getRGB()
            {
               final ClipLauncherSlot s = getSlot();

               return getClipColor(s);
            }

            @Override
            public boolean get()
            {
               return getSlot().hasContent().get();
            }

            @Override
            public void set(final boolean pressed)
            {
               getSlot().select();
               getSlot().launch();
            }
         });

         mStepsZoomLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               final int numStepPages = getNumStepPages();

               final int playingPage = mCursorClip.playingStep().get() / 16;

               if (padIndex < numStepPages)
               {
                  float[] clipColor = RGBButtonTarget.getFromValue(mCursorClip.color());

                  if (padIndex != mCurrentPageForSteps)
                     clipColor = RGBButtonTarget.mix(clipColor, BLACK, 0.5f);

                  if (padIndex == playingPage)
                     return RGBButtonTarget.mix(clipColor, WHITE, 1 - getTransportPulse(1.0, 1));

                  return clipColor;
               }

               return BLACK;
            }

            @Override
            public boolean get()
            {
               return true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  mCurrentPageForSteps = padIndex;
                  mCursorClip.scrollToStep(16 * mCurrentPageForSteps);
               }
            }
         });

         mStepsSetupLoopLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               if (padIndex == 14 || padIndex == 15)
               {
                  return WHITE;
               }

               final int numStepPages = getNumStepPages();

               final int playingPage = mCursorClip.playingStep().get() / 16;

               if (padIndex < numStepPages)
               {
                  final float[] clipColor = RGBButtonTarget.getFromValue(mCursorClip.color());

                  if (padIndex == playingPage)
                     return RGBButtonTarget.mix(clipColor, WHITE, 1 - getTransportPulse(1.0, 1));

                  return clipColor;
               }

               return BLACK;
            }

            @Override
            public boolean get()
            {
               return true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (padIndex == 14)
               {
                  mCursorClip.getLoopLength().set(Math.max(getPageLengthInBeatTime(),
                     mCursorClip.getLoopLength().get() - getPageLengthInBeatTime()));
               }
               else if (padIndex == 15)
               {
                  mCursorClip.getLoopLength()
                     .set(mCursorClip.getLoopLength().get() + getPageLengthInBeatTime());
               }
               else
               {
                  // mCursorClip.getLoopStart().set(padIndex * getPageLengthInBeatTime());
               }
            }
         });
      }

      final double timings[] = { 1, 1.0 / 2, 1.0 / 4, 1.0 / 8, 3.0 / 4.0, 3.0 / 8.0, 3.0 / 16.0, 3.0 / 32.0 };

      for (int i = 0; i < 8; i++)
      {
         final double timing = timings[i];

         mNoteRepeatShiftLayer.bind(pads[i], new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               return mArpeggiator.period().get() == timing ? RED : DIM_RED;
            }

            @Override
            public boolean get()
            {
               return true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
                  mArpeggiator.period().set(timing);
            }
         });

         mNoteRepeatShiftLayer.bind(pads[i + 8], new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               return BLACK;
            }

            @Override
            public boolean get()
            {
               return false;
            }

            @Override
            public void set(final boolean pressed)
            {

            }
         });
      }

      mCursorClip.playingStep().addValueObserver(s -> mPlayingStep = s, -1);
      mCursorClip.scrollToKey(36);
      mCursorClip.addNoteStepObserver(d -> {
         final int x = d.x();
         final int y = d.y();

         if (y == 0 && x >= 0 && x < mStepData.length)
         {
            final NoteStep.State state = d.state();

            if (state == NoteStep.State.NoteOn)
               mStepData[x] = 2;
            else if (state == NoteStep.State.NoteSustain)
               mStepData[x] = 1;
            else
               mStepData[x] = 0;
         }
      });
      mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);
   }

   private double getPageLengthInBeatTime()
   {
      return 4;
   }

   private float[] mixColorWithWhite(final float[] color, final int velocity)
   {
      final float x = velocity / 127.f;
      final float[] mixed = new float[3];
      for (int i = 0; i < 3; i++)
         mixed[i] = color[i] * (1 - x) + x;

      return mixed;
   }

   private HardwareButton createCCButton(final int controlNumber)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton();
      final MidiExpressions midiExpressions = getHost().midiExpressions();

      button.pressedAction().setActionMatcher(mMidiIn
         .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, controlNumber, 0));

      return button;
   }

   private void initButtons()
   {
      mTransport.isPlaying().markInterested();

      final Button shiftButton = addElement(new Button(CC_SHIFT));
      final ButtonTarget shiftTarget = new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mShift;
         }

         @Override
         public void set(final boolean pressed)
         {
            mShift = pressed;
         }
      };
      mBaseLayer.bind(shiftButton, shiftTarget);
      mStepsLayer.bind(shiftButton, shiftTarget);

      final Button clickToggle = addElement(new Button(CC_CLICK_COUNT_IN));
      mBaseLayer.bindToggle(clickToggle, mTransport.isMetronomeEnabled());

      final Button playButton = addElement(new Button(CC_PLAY_LOOP_TOGGLE));
      mBaseLayer.bindPressedRunnable(playButton, mTransport.isPlaying(), () -> {
         if (mShift)
            mTransport.isArrangerLoopEnabled().toggle();
         else
            mTransport.togglePlay();
      });

      final Button stopButton = addElement(new Button(CC_STOP_UNDO));
      mBaseLayer.bind(stopButton, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return !mTransport.isPlaying().get();
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed)
            {
               if (mShift)
                  mApplication.undo();
               else
                  mTransport.stop();
            }
         }
      });

      final Button recordButton = addElement(new Button(CC_RECORD_SAVE));
      mBaseLayer.bindPressedRunnable(recordButton, mTransport.isArrangerRecordEnabled(), () -> {
         if (mShift)
            save();
         else
            mTransport.isArrangerRecordEnabled().toggle();
      });

      final Button upButton = addElement(new Button(CC_UP));
      mBaseLayer.bindPressedRunnable(upButton, mCursorTrack.hasPrevious(), mCursorTrack::selectPrevious);
      mStepsLayer.bindPressedRunnable(upButton, mCursorClip.canScrollKeysUp(), () -> scrollKeys(1));
      final Button downButton = addElement(new Button(CC_DOWN));
      mBaseLayer.bindPressedRunnable(downButton, mCursorTrack.hasNext(), mCursorTrack::selectNext);
      mStepsLayer.bindPressedRunnable(downButton, mCursorClip.canScrollKeysDown(), () -> scrollKeys(-1));
      final Button leftButton = addElement(new Button(CC_LEFT));
      mBaseLayer.bindPressedRunnable(leftButton, mCursorDevice.hasPrevious(), mCursorDevice::selectPrevious);
      mStepsLayer.bindPressedRunnable(leftButton, mCursorClip.canScrollStepsBackwards(),
         () -> scrollPage(-1));
      final Button rightButton = addElement(new Button(CC_RIGHT));
      mBaseLayer.bindPressedRunnable(rightButton, mCursorDevice.hasNext(), mCursorDevice::selectNext);
      mStepsLayer.bindPressedRunnable(rightButton, mCursorClip.canScrollStepsForwards(), () -> scrollPage(1));

      final RGBButton selectButton = addElement(new RGBButton(CC_SELECT));
      mBaseLayer.bind(selectButton, new RGBButtonTarget()
      {
         @Override
         public boolean get()
         {
            return true;
         }

         @Override
         public float[] getRGB()
         {
            return getClipColor(mCursorClip.clipLauncherSlot());
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed)
            {
               if (mCursorClip.clipLauncherSlot().isRecording().get())
               {
                  mCursorClip.clipLauncherSlot().launch();
               }
               else
                  activateLayer(mLauncherClipsLayer);
            }
            else
               deactivateLayer(mLauncherClipsLayer);
         }
      });

      final Button zoomButton = addElement(new Button(CC_ZOOM));
      mStepsLayer.bindLayerGate(this, zoomButton, mStepsZoomLayer);

      final Button setupButton = addElement(new Button(CC_SETUP));
      final Button setLoopButton = addElement(new Button(CC_SET_LOOP));
      mStepsLayer.bindLayerGate(this, setLoopButton, mStepsSetupLoopLayer);

      final Button editorToggle = addElement(new Button(CC_EDITOR));
      mBaseLayer.bind(editorToggle, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return isLayerActive(mStepsLayer);
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed)
            {
               toggleLayer(mStepsLayer);
            }
         }
      });

      final Button noteRepeat = addElement(new Button(CC_NOTE_REPEAT));
      mBaseLayer.bind(noteRepeat, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mArpeggiator.isEnabled().get();
         }

         @Override
         public void set(final boolean pressed)
         {
            if (!pressed)
            {
               mArpeggiator.mode().set("all");
               mArpeggiator.usePressureToVelocity().set(true);

               toggleLayer(mNoteRepeatLayer);

               final boolean wasEnabled = mArpeggiator.isEnabled().get();

               mArpeggiator.isEnabled().set(!wasEnabled);

               if (wasEnabled)
                  mNoteRepeatLayer.deactivate();
               else
                  mNoteRepeatLayer.activate();
            }
         }
      });

      mNoteRepeatLayer.bindLayerGate(this, shiftButton, mNoteRepeatShiftLayer);

      final Button fullVelocity = addElement(new Button(CC_FULL_LEVEL));
      mBaseLayer.bind(fullVelocity, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mIsOn;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed)
            {
               mIsOn = !mIsOn;
               mNoteInput.setVelocityTranslationTable(
                  mIsOn ? NoteInputUtils.FULL_VELOCITY : NoteInputUtils.NORMAL_VELOCITY);
            }
         }

         private boolean mIsOn;
      });

      activateLayer(mBaseLayer);
   }

   private void scrollKeys(final int delta)
   {
      mCurrentPadForSteps = (mCurrentPadForSteps + delta) & 0xf;
      mCursorClip.scrollToKey(36 + mCurrentPadForSteps);
   }

   private void scrollPage(final int delta)
   {
      mCurrentPageForSteps += delta;
      mCurrentPageForSteps = Math.max(0, Math.min(mCurrentPageForSteps, getNumStepPages() - 1));
      mCursorClip.scrollToStep(16 * mCurrentPageForSteps);
   }

   private int getNumStepPages()
   {
      return (int)Math.ceil(
         (mCursorClip.getLoopStart().get() + mCursorClip.getLoopLength().get()) / (16.0 * getStepSize()));
   }

   private double getStepSize()
   {
      return 0.25;
   }

   private void initEncoders()
   {
      for (int i = 0; i < 4; i++)
      {
         final SettableRangedValue parameterValue = mRemoteControls.getParameter(i).value();
         final Encoder encoder = addElement(new Encoder(CC_ENCODER_1 + i));
         mBaseLayer.bind(encoder, new EncoderTarget()
         {
            @Override
            public void inc(final int steps)
            {
               parameterValue.inc(steps, mShift ? 1010 : 101);
            }
         });
      }
   }

   private void save()
   {
      final Action saveAction = mApplication.getAction("Save");
      if (saveAction != null)
      {
         saveAction.invoke();
      }
   }

   float[] getClipColor(final ClipLauncherSlot s)
   {
      if (s.isRecordingQueued().get())
      {
         return RGBButtonTarget.mix(RED, BLACK, getTransportPulse(1.0, 1));
      }
      else if (s.hasContent().get())
      {
         if (s.isPlaybackQueued().get())
         {
            return RGBButtonTarget.mixWithValue(s.color(), WHITE, 1 - getTransportPulse(4.0, 1));
         }
         else if (s.isRecording().get())
         {
            return RED;
         }
         else if (s.isPlaying().get() && mTransport.isPlaying().get())
         {
            return RGBButtonTarget.mixWithValue(s.color(), WHITE, 1 - getTransportPulse(1.0, 1));
         }

         return RGBButtonTarget.getFromValue(s.color());
      }
      else if (mCursorTrack.arm().get())
      {
         return RGBButtonTarget.mix(BLACK, RED, 0.1f);
      }

      return BLACK;
   }

   private float getTransportPulse(final double multiplier, final double amount)
   {
      final double p = mTransport.getPosition().get() * multiplier;
      return (float)((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
   }

   /* API Objects */
   private CursorTrack mCursorTrack;

   private PinnableCursorDevice mCursorDevice;

   private CursorRemoteControlsPage mRemoteControls;

   private Transport mTransport;

   private MidiIn mMidiIn;

   private MidiOut mMidiOut;

   private Application mApplication;

   private DrumPadBank mDrumPadBank;

   private boolean mShift;

   private NoteInput mNoteInput;

   private PlayingNote[] mPlayingNotes;

   private float[][] mDrumPadColors;

   private Clip mCursorClip;

   private int mPlayingStep;

   private int[] mStepData = new int[16];

   private int mCurrentPadForSteps;

   private int mCurrentPageForSteps;

   private HardwareSurface mHardwareSurface;

   private HardwareButton mShiftButton, mUpButton, mDownButton, mLeftButton, mRightButton, mSelectButton,
      mZoomButton, mClickCountInButton, mRecordSaveButton, mPlayLoopButton, mStopUndoButton, mSetupButton,
      mSetLoopButton, mEditorButton, mNudgeQuantizeButton, mShowHideButton, mPresetPadSelectButton, mBankButton,
      mFullLevelButton, mNoteRepeatButton;

   private final Layers mLayers = new Layers(this);

   private Layer mStepsLayer = createLayer("Steps");

   private Layer mLauncherClipsLayer = createLayer("Launcher Clips");

   private Layer mBaseLayer = createLayer("Base");

   private Layer mNoteRepeatLayer = createLayer("Note Repeat");

   private Layer mNoteRepeatShiftLayer = createLayer("Note Repeat Shift");

   private Layer mStepsZoomLayer = createLayer("Steps Zoom");

   private Layer mStepsSetupLoopLayer = createLayer("Steps Setup Loop");

   private Arpeggiator mArpeggiator;

   private SceneBank mSceneBank;
}