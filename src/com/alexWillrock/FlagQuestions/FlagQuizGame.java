package com.alexWillrock.FlagQuestions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.deitel.flagquizgame.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FlagQuizGame extends Activity 
{
   private static final String TAG = "FlagQuizGame Activity";
   
   private List<String> fileNameList; // название флагов
   private List<String> quizCountriesList; // название стран
   private Map<String, Boolean> regionsMap; // континенты которые могут быть активны и не активны
   private String correctAnswer; // выбран правильный ответ
   private int totalGuesses; // колличество сделанных предположений
   private int correctAnswers; // колличество правильных ответов
   private int guessRows; // числов строк в таблице предположений
   private Random random;
   private Handler handler; // задержка
   private Animation shakeAnimation; // анимация
   
   private TextView answerTextView; // показать уведомление правильно или неправильно
   private TextView questionNumberTextView; // номер текущего вопроса
   private ImageView flagImageView; // изображение флага
   private TableLayout buttonTableLayout; // таблица ответов

   @Override
   public void onCreate(Bundle savedInstanceState) 
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      fileNameList = new ArrayList<String>(); // массив названий файлов
      quizCountriesList = new ArrayList<String>(); // массив стран в игре, набирается случайным образом
      regionsMap = new HashMap<String, Boolean>();
      guessRows = 1; // по умолчанию 1 строка ответов
      random = new Random();
      handler = new Handler();

      shakeAnimation = 
         AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
      shakeAnimation.setRepeatCount(3); // трижды

      String[] regionNames = 
         getResources().getStringArray(R.array.regionsList); // берем континенты

      for (String region : regionNames )
         regionsMap.put(region, true); // по умолчанию все всключены

      questionNumberTextView = 
         (TextView) findViewById(R.id.questionNumberTextView);
      flagImageView = (ImageView) findViewById(R.id.flagImageView);
      buttonTableLayout = 
         (TableLayout) findViewById(R.id.buttonTableLayout);
      answerTextView = (TextView) findViewById(R.id.answerTextView);

      questionNumberTextView.setText(
         getResources().getString(R.string.question) + " 1 " +
         getResources().getString(R.string.of) + " 10");

      resetQuiz(); // начать розыгрыш
   }

   private void resetQuiz() // розыгрыш
   {      

      AssetManager assets = getAssets();
      fileNameList.clear();
      
      try 
      {
         Set<String> regions = regionsMap.keySet(); // get Set of regions

         // loop through each region
         for (String region : regions) 
         {
            if (regionsMap.get(region)) // если континенты выбраны
            {
               String[] paths = assets.list(region); // получаем название всех континентов

               for (String path : paths) 
                  fileNameList.add(path.replace(".png", "")); // добавляем все флаги, убирая разширение
            }
         }
      }
      catch (IOException e) 
      {
         Log.e(TAG, "Изображение не загрузилось", e);
      }
      
      correctAnswers = 0;
      totalGuesses = 0;
      quizCountriesList.clear();

      int flagCounter = 1; 
      int numberOfFlags = fileNameList.size();

      while (flagCounter <= 10) // выбираем случайно 10 стран
      {
         int randomIndex = random.nextInt(numberOfFlags);

         String fileName = fileNameList.get(randomIndex);

         if (!quizCountriesList.contains(fileName)) // если регион включен и не выбран, добавляем его
         {
            quizCountriesList.add(fileName);
            ++flagCounter;
         }
      }

      loadNextFlag(); // загружаем изображение флага
   }

   private void loadNextFlag() 
   {
      String nextImageName = quizCountriesList.remove(0);
      correctAnswer = nextImageName;

      answerTextView.setText("");

      questionNumberTextView.setText( // указываем номер вопроса
         getResources().getString(R.string.question) + " " +
         (correctAnswers + 1) + " " + 
         getResources().getString(R.string.of) + " 10");

      String region =
         nextImageName.substring(0, nextImageName.indexOf('-')); // загружаем изображение, название до черточки

      AssetManager assets = getAssets();
      InputStream stream;

      try
      {
         stream = assets.open(region + "/" + nextImageName + ".png"); // добавляем в поток изображение

         Drawable flag = Drawable.createFromStream(stream, nextImageName);
         flagImageView.setImageDrawable(flag);                       // рисуем изображение
      }
      catch (IOException e)  
      {
         Log.e(TAG, "Картинка не не загружена" + nextImageName, e);
      }

      for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) //удаляем таблицу с вариантами
         ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

      Collections.shuffle(fileNameList); // перемешиваем названия

      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct)); // добавляем верный ответ

      LayoutInflater inflater = (LayoutInflater) getSystemService(
         Context.LAYOUT_INFLATER_SERVICE);

      for (int row = 0; row < guessRows; row++) // добавляем таблицу с вариантами
      {
         TableRow currentTableRow = getTableRow(row);

         for (int column = 0; column < 3; column++) 
         {
            Button newGuessButton = 
               (Button) inflater.inflate(R.layout.guess_button, null); // догружаем кнопки

            String fileName = fileNameList.get((row * 3) + column);
            newGuessButton.setText(getCountryName(fileName)); // добавляем названия

            newGuessButton.setOnClickListener(guessButtonListener); // прослушка для кнопок
            currentTableRow.addView(newGuessButton);
         }
      }

      // добавляю кнопку правильного ответа
      int row = random.nextInt(guessRows); // случайная строка
      int column = random.nextInt(3); // случайная колонка
      TableRow randomTableRow = getTableRow(row); // вставить в таблицу
      String countryName = getCountryName(correctAnswer);
      ((Button)randomTableRow.getChildAt(column)).setText(countryName);    
   }

   private TableRow getTableRow(int row)
   {
      return (TableRow) buttonTableLayout.getChildAt(row);
   }

   private String getCountryName(String name) //взять название страны
   {
      return name.substring(name.indexOf('-') + 1).replace('_', ' ');
   }
   
   // при выборе ответа
   private void submitGuess(Button guessButton)
   {
      String guess = guessButton.getText().toString();
      String answer = getCountryName(correctAnswer);
      ++totalGuesses; // прибавить номер попытки
      
      // если верно
      if (guess.equals(answer)) 
      {
         ++correctAnswers; // добавить верный ответ

         answerTextView.setText(answer + "!"); // показать верно
         answerTextView.setTextColor(
            getResources().getColor(R.color.correct_answer));

         disableButtons(); // отключить
         
         // если собрал 10 ответов
         if (correctAnswers == 10) 
         {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.reset_quiz); // заголовок

            builder.setMessage(String.format("%d %s, %.02f%% %s", 
               totalGuesses, getResources().getString(R.string.guesses),
               (1000 / (double) totalGuesses), 
               getResources().getString(R.string.correct)));

            builder.setCancelable(false); 
            
            // добавить кнопку перезагрузки
            builder.setPositiveButton(R.string.reset_quiz,
               new DialogInterface.OnClickListener()                
               {                                                       
                  public void onClick(DialogInterface dialog, int id) 
                  {
                     resetQuiz();                                      
                  }
               }
            );

            AlertDialog resetDialog = builder.create();
            resetDialog.show();
         }
         else // если игра не закончена
         {
            // перейти к следующему флагу с задержкой
            handler.postDelayed(
               new Runnable()
               { 
                  @Override
                  public void run()
                  {
                     loadNextFlag();
                  }
               }, 1000); // 1 секунда отдыха
         }
      }
      else // неверный ответ
      {
         // запустить анимашку
         flagImageView.startAnimation(shakeAnimation);

         // сообщение о неверном ответе
         answerTextView.setText(R.string.incorrect_answer);
         answerTextView.setTextColor(
            getResources().getColor(R.color.incorrect_answer));
         guessButton.setEnabled(false); // заблокировать ошибочную кнопку
      }
   }

   // отключение кнопок
   private void disableButtons()
   {
      for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
      {
         TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
         for (int i = 0; i < tableRow.getChildCount(); ++i)
            tableRow.getChildAt(i).setEnabled(false);
      }
   }

   private final int CHOICES_MENU_ID = Menu.FIRST;
   private final int REGIONS_MENU_ID = Menu.FIRST + 1;

   //вызов меню
   @Override
   public boolean onCreateOptionsMenu(Menu menu)             
   {            
      super.onCreateOptionsMenu(menu);                        
                                                              
      // пункты - континенты и колличество ответов
      menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
      menu.add(Menu.NONE, REGIONS_MENU_ID, Menu.NONE, R.string.regions);
                                                              
      return true;
   }

   // когда выбран пункт
   @Override
   public boolean onOptionsItemSelected(MenuItem item) 
   {
      // получаем ид пункта
      switch (item.getItemId()) 
      {
         case CHOICES_MENU_ID:
            final String[] possibleChoices = 
               getResources().getStringArray(R.array.guessesList);

            AlertDialog.Builder choicesBuilder = 
               new AlertDialog.Builder(this);         
            choicesBuilder.setTitle(R.string.choices);

            choicesBuilder.setItems(R.array.guessesList,
               new DialogInterface.OnClickListener()                    
               {                                                        
                  public void onClick(DialogInterface dialog, int item) 
                  {
                     guessRows = Integer.parseInt(
                        possibleChoices[item].toString()) / 3;          
                     resetQuiz();
                  }
               }
            );

            AlertDialog choicesDialog = choicesBuilder.create();
            choicesDialog.show();
            return true; 

         case REGIONS_MENU_ID:
            // названия континентов
            final String[] regionNames = 
               regionsMap.keySet().toArray(new String[regionsMap.size()]);
         
            // если включены
            boolean[] regionsEnabled = new boolean[regionsMap.size()];
            for (int i = 0; i < regionsEnabled.length; ++i)
               regionsEnabled[i] = regionsMap.get(regionNames[i]);

            AlertDialog.Builder regionsBuilder =
               new AlertDialog.Builder(this);
            regionsBuilder.setTitle(R.string.regions);
            
            // удаляем лишние символы
            String[] displayNames = new String[regionNames.length];
            for (int i = 0; i < regionNames.length; ++i)
               displayNames[i] = regionNames[i].replace('_', ' ');
         

            regionsBuilder.setMultiChoiceItems( 
               displayNames, regionsEnabled,
               new DialogInterface.OnMultiChoiceClickListener() 
               {
                  @Override
                  public void onClick(DialogInterface dialog, int which,
                     boolean isChecked) 
                  {
                     regionsMap.put(
                        regionNames[which].toString(), isChecked);
                  }
               }
            );
          
            // перезагрузка игры по диалогу
            regionsBuilder.setPositiveButton(R.string.reset_quiz,
               new DialogInterface.OnClickListener()
               {
                  @Override
                  public void onClick(DialogInterface dialog, int button)
                  {
                     resetQuiz(); // перезагрузить игру
                  }
               }
            );

            AlertDialog regionsDialog = regionsBuilder.create();
            regionsDialog.show();
            return true;
      }

      return super.onOptionsItemSelected(item);
   }

   // слушатель нажатия кнопки
   private OnClickListener guessButtonListener = new OnClickListener() 
   {
      @Override
      public void onClick(View v) 
      {
         submitGuess((Button) v); // нажал кнопку
      }
   };
}