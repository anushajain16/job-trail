import { Field, Select } from '@/components/ui/field'
import { useToast } from '@/components/ui/toast'
import { ALL_STAGES } from '@/lib/design'
import type { Stage, Uuid } from '@/api/types'
import { useChangeStage } from './hooks'

/**
 * Move a line to another station. Every transition is legal except a no-op
 * (the backend 400s on "changing" to the current stage), so the current
 * stage is rendered but disabled.
 */
export function StageChanger({
  applicationId,
  currentStage,
}: {
  applicationId: Uuid
  currentStage: Stage
}) {
  const changeStage = useChangeStage()
  const { notify, notifyError } = useToast()

  return (
    <Field label="Move to station">
      {(props) => (
        <Select
          {...props}
          value={currentStage}
          disabled={changeStage.isPending}
          onChange={async (event) => {
            const stage = event.target.value as Stage
            if (stage === currentStage) return
            try {
              await changeStage.mutateAsync({ id: applicationId, stage })
              notify(`Moved to ${stage}.`, 'success')
            } catch (error) {
              notifyError(error)
            }
          }}
          options={ALL_STAGES.map((stage) => ({
            value: stage,
            label: stage === currentStage ? `${stage} (current)` : stage,
          }))}
        />
      )}
    </Field>
  )
}
