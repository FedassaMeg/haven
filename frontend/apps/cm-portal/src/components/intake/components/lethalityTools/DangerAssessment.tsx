/**
 * Danger Assessment Tool
 *
 * Jacquelyn Campbell, Ph.D., R.N., Johns Hopkins University
 * 20-question validated instrument for assessing lethality risk in DV situations
 */

import React, { useState, useEffect } from 'react';
import type { LethalityScreeningResponses, RiskLevel } from '../../utils/types';

interface DangerAssessmentProps {
  responses: LethalityScreeningResponses;
  onChange: (responses: LethalityScreeningResponses) => void;
  onScoreCalculated: (score: number, maxScore: number, riskLevel: RiskLevel) => void;
}

const DANGER_ASSESSMENT_QUESTIONS = [
  {
    id: 'DA1',
    question: 'Has the physical violence increased in severity or frequency over the past year?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA2',
    question: 'Does your partner own a gun?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA3',
    question: 'Have you left your partner after living together during the past year?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA4',
    question: 'Is your partner unemployed?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA5',
    question: 'Has your partner ever tried to choke you?',
    weight: 1,
    highRisk: true,
  },
  {
    id: 'DA6',
    question: 'Has your partner ever threatened to kill you?',
    weight: 1,
    highRisk: true,
  },
  {
    id: 'DA7',
    question: 'Do you believe your partner is capable of killing you?',
    weight: 1,
    highRisk: true,
  },
  {
    id: 'DA8',
    question: 'Does your partner follow or spy on you, leave threatening notes, or destroy your property?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA9',
    question: 'Has your partner ever forced you to have sex when you did not wish to do so?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA10',
    question: 'Has your partner ever tried to kill you?',
    weight: 1,
    highRisk: true,
  },
  {
    id: 'DA11',
    question: 'Does your partner have a problem with drugs or alcohol?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA12',
    question: 'Does your partner control most or all of your daily activities?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA13',
    question: 'Is your partner violently and constantly jealous of you?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA14',
    question: 'Have you ever been beaten by your partner when you were pregnant?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA15',
    question: 'Has your partner ever threatened or tried to commit suicide?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA16',
    question: 'Does your partner threaten to harm your children?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA17',
    question: 'Do you believe your partner has ever spied on you with technology?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA18',
    question: 'Is there anything else that worries you about your safety?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA19',
    question: 'Has your partner ever used a weapon against you or threatened you with a weapon?',
    weight: 1,
    highRisk: false,
  },
  {
    id: 'DA20',
    question: 'Do you have a child that your partner knows is not theirs?',
    weight: 1,
    highRisk: false,
  },
];

export const DangerAssessment: React.FC<DangerAssessmentProps> = ({
  responses,
  onChange,
  onScoreCalculated,
}) => {
  const [localResponses, setLocalResponses] = useState<LethalityScreeningResponses>(responses || {});

  useEffect(() => {
    setLocalResponses(responses || {});
  }, [responses]);

  const handleResponseChange = (questionId: string, answer: boolean) => {
    const question = DANGER_ASSESSMENT_QUESTIONS.find(q => q.id === questionId);
    if (!question) return;

    const updatedResponses = {
      ...localResponses,
      [questionId]: {
        question: question.question,
        answer,
        weight: question.weight,
      },
    };

    setLocalResponses(updatedResponses);
    onChange(updatedResponses);

    // Calculate score
    calculateScore(updatedResponses);
  };

  const calculateScore = (currentResponses: LethalityScreeningResponses) => {
    let score = 0;
    const highRiskIndicators: string[] = [];

    DANGER_ASSESSMENT_QUESTIONS.forEach(question => {
      const response = currentResponses[question.id];
      if (response && response.answer === true) {
        score += question.weight;
        if (question.highRisk) {
          highRiskIndicators.push(question.question);
        }
      }
    });

    // Determine risk level based on score
    let riskLevel: RiskLevel;
    if (score >= 18) {
      riskLevel = 'SEVERE';
    } else if (score >= 14) {
      riskLevel = 'HIGH';
    } else if (score >= 8) {
      riskLevel = 'MODERATE';
    } else {
      riskLevel = 'LOW';
    }

    onScoreCalculated(score, 20, riskLevel);
  };

  const answeredCount = Object.keys(localResponses).length;
  const totalQuestions = DANGER_ASSESSMENT_QUESTIONS.length;
  const progressPercent = (answeredCount / totalQuestions) * 100;

  const score = DANGER_ASSESSMENT_QUESTIONS.reduce((total, q) => {
    const response = localResponses[q.id];
    return total + (response?.answer === true ? q.weight : 0);
  }, 0);

  const highRiskIndicators = DANGER_ASSESSMENT_QUESTIONS.filter(
    q => q.highRisk && localResponses[q.id]?.answer === true
  );

  return (
    <div className="lethality-tool danger-assessment">
      <div className="tool-header">
        <h4>Danger Assessment</h4>
        <p className="tool-description">
          This assessment helps identify the level of danger an abuse victim is in. Please answer
          all questions honestly.
        </p>
      </div>

      <div className="progress-indicator">
        <div className="progress-bar">
          <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
        </div>
        <span className="progress-text">
          {answeredCount} of {totalQuestions} questions answered
        </span>
      </div>

      <div className="questions-list">
        {DANGER_ASSESSMENT_QUESTIONS.map((question, index) => (
          <div
            key={question.id}
            className={`question-item ${question.highRisk ? 'high-risk-question' : ''}`}
          >
            <div className="question-header">
              <span className="question-number">{index + 1}</span>
              <span className="question-text">
                {question.question}
                {question.highRisk && <span className="high-risk-badge">High Risk Indicator</span>}
              </span>
            </div>

            <div className="question-answers">
              <label className="radio-label">
                <input
                  type="radio"
                  name={question.id}
                  checked={localResponses[question.id]?.answer === true}
                  onChange={() => handleResponseChange(question.id, true)}
                />
                <span>Yes</span>
              </label>

              <label className="radio-label">
                <input
                  type="radio"
                  name={question.id}
                  checked={localResponses[question.id]?.answer === false}
                  onChange={() => handleResponseChange(question.id, false)}
                />
                <span>No</span>
              </label>
            </div>
          </div>
        ))}
      </div>

      {answeredCount === totalQuestions && (
        <div className="assessment-summary">
          <div className="score-display">
            <span className="score-label">Total Score:</span>
            <span className="score-value">
              {score} / {totalQuestions}
            </span>
          </div>

          {highRiskIndicators.length > 0 && (
            <div className="high-risk-summary">
              <h5>High-Risk Indicators Identified:</h5>
              <ul>
                {highRiskIndicators.map((q, i) => (
                  <li key={i}>{q.question}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="score-interpretation">
            <h5>Score Interpretation:</h5>
            <ul>
              <li>
                <strong>0-7:</strong> Variable Danger (LOW)
              </li>
              <li>
                <strong>8-13:</strong> Increased Danger (MODERATE)
              </li>
              <li>
                <strong>14-17:</strong> Severe Danger (HIGH)
              </li>
              <li>
                <strong>18-20:</strong> Extreme Danger (SEVERE)
              </li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};
