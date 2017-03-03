package com.niemisami.wedu.course;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.niemisami.wedu.R;
import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.utils.WeduDateUtils;

import java.util.List;

import static android.media.CamcorderProfile.get;

/**
 * Created by Sami on 23.2.2017.
 */

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionAdapterViewHolder> {

    private List<Question> mQuestions;
    private int[] mQuestionColors;
    private Context mContext;


    private final QuestionOnClickHandler mQuestionOnClickHandler;

    public interface QuestionOnClickHandler {
        void onUpvoteClick(int itemPosition);

        void onDownvoteClick(int itemPosition);

        void onQuestionClick(int itemPosition);
    }

    public QuestionsAdapter(Context context, QuestionOnClickHandler questionOnClickHandler) {
        mContext = context;
        mQuestionOnClickHandler = questionOnClickHandler;
        mQuestionColors = context.getResources().getIntArray(R.array.username_colors);
    }

    public void setQuestions(List<Question> questions) {
        mQuestions = questions;
        notifyDataSetChanged();
    }

    @Override
    public QuestionAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Question.TYPE_MESSAGE_QUESTION:
                layout = R.layout.item_question;
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        return new QuestionAdapterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(QuestionAdapterViewHolder viewHolder, int position) {
        Question question = mQuestions.get(position);
        viewHolder.setQuestionMessage(question.getMessage());
        viewHolder.setCreated(question.getCreated());
        viewHolder.displayAsSolved(question.isSolved());
        viewHolder.setUpvotes(question.getUpvotes());
    }

    @Override
    public int getItemCount() {
        return mQuestions.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mQuestions.get(position).getType();
    }

    public class QuestionAdapterViewHolder extends RecyclerView.ViewHolder {
        private TextView mCreatedView, mQuestionView, mUpvotesView;
        private ImageView mSolvedIcon;
        private ImageButton mUpvoteButton, mDownvoteButton;

        public QuestionAdapterViewHolder(View itemView) {
            super(itemView);

            mCreatedView = (TextView) itemView.findViewById(R.id.question_created);
            mQuestionView = (TextView) itemView.findViewById(R.id.question_message);
            mUpvotesView = (TextView) itemView.findViewById(R.id.label_upvotes);
            mSolvedIcon = (ImageView) itemView.findViewById(R.id.icon_solved);
            mUpvoteButton = (ImageButton) itemView.findViewById(R.id.button_upvote_question);
            mDownvoteButton = (ImageButton) itemView.findViewById(R.id.button_downvote_question);

            itemView.setOnClickListener(onQuestionClickListener);
            mUpvoteButton.setOnClickListener(onUpvoteClickListener);
            mDownvoteButton.setOnClickListener(onDownvoteClickListener);
        }

        public void displayAsSolved(boolean solved) {
            int displayIcon = (solved ? View.VISIBLE : View.INVISIBLE);
            mSolvedIcon.setVisibility(displayIcon);
        }

        public void setCreated(long dateMillis) {
            if (null == mCreatedView) return;

            long normalizedDate = WeduDateUtils.normalizeDate(dateMillis);
            String dateString = WeduDateUtils.getFriendlyDateString(mContext, normalizedDate, true);
            mCreatedView.setText(dateString);
        }

        public void setQuestionMessage(String message) {
            if (null == mQuestionView) return;
            mQuestionView.setText(message);
            View parent = (View) mCreatedView.getParent();
            parent.setBackgroundColor(getQuestionBackgroundColor(message.substring(0,message.length())));
        }

        public void setUpvotes(int upvotes) {
            mUpvotesView.setText(Integer.toString(upvotes));
        }

        private int getQuestionBackgroundColor(String hashableString) {
            int hash = 7;
            for (int i = 0, len = hashableString.length(); i < len; i++) {
                hash = hashableString.codePointAt(i) + (hash << 5) - hash;
            }
            int index = Math.abs(hash % mQuestionColors.length);
            return mQuestionColors[index];
        }


        /**
         * ClickListeners for question item
         */

        private View.OnClickListener onQuestionClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuestionOnClickHandler.onQuestionClick(getAdapterPosition());
            }
        };
        private View.OnClickListener onDownvoteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuestionOnClickHandler.onDownvoteClick(getAdapterPosition());
            }
        };
        private View.OnClickListener onUpvoteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuestionOnClickHandler.onUpvoteClick(getAdapterPosition());
            }
        };


    }
}