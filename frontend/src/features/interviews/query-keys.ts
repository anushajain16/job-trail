export const interviewRoundKeys = {
  all: ['interviewRounds'] as const,
  list: (applicationId: string) => [...interviewRoundKeys.all, 'list', applicationId] as const,
}
